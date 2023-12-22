package edu.kit.aifb.solid.wac.query;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.ErrorHandlerFactory;

import edu.kit.aifb.solid.wac.Namespaces;
import edu.kit.aifb.solid.wac.WacMapping;

/**
 * A builder-pattern for ACL Queries ({@link WacQueryType}).
 */
public class WacQueryBuilder {

    /**
     *
     * Get a query builder in the current application environment:
     *
     * @param envResourceMap a mapping of URI string to RDF datasets, s.t. the
     * query may look up data, .acl or specific agentGroups.
     * @param envResourceAclMap a mapping of URI string of a resource to its
     * corresponding .acl
     * @return a new builder (for the provided environment)
     */
    public static WacQueryBuilder newBuilder(Map<String, Dataset> envResourceMap, WacMapping envResourceAclMap) {
        return new WacQueryBuilder(envResourceMap, envResourceAclMap);
    }

    private WacQueryBuilder(Map<String, Dataset> resourceMap, WacMapping resourceAclMap) {
        this.resourceMap = resourceMap;
        this.resourceAclMap = resourceAclMap;
    }

    // RESOURCE DATA (-> for lookup of resources, e.g. .acl and resources of agentGroup)
    private final Map<String, Dataset> resourceMap;

    // RESOURCE-ACL MAP (-> isForControlRequest)
    private final WacMapping resourceAclMap;

    // REQUEST DATA
    private String resource;
    private String method;
    private String body;
    private String webid;

    // REQUEST PROPERTIES
    private boolean isForControlRequest = false;
    private boolean isForNonDeletingRequest = false;
    private String accessMode;

    // ACL DATA
    Dataset authoritativeACL;
    String onResource;
    boolean hasInheritedRule = false;

    /**
     * Set the action used to access the resource.
     *
     * @param resource
     * @param method
     * @param body
     * @return the builder
     */
    public WacQueryBuilder forRequest(String resource, String method, String body) {
        this.resource = resource;
        this.method = method;
        this.body = body;
        this.isForControlRequest = this.checkForControlRequest();
        this.isForNonDeletingRequest = this.checkForNonDeletingRequest();
        this.accessMode = this.getAccessMode();
        return this;
    }

    /**
     * Set the webid of the accessing agent. Remains {@code null} if unknown.
     *
     * @param webid
     * @return the builder
     */
    public WacQueryBuilder byAgent(String webid) {
        this.webid = webid;
        return this;
    }

    /**
     * If the target {@code resource} of the request is the {@code .acl} itself,
     * then {@code acl:mode acl:Control} is required.
     *
     * @return isControlling
     */
    private boolean checkForControlRequest() {
        return this.resource.equals(this.resourceAclMap.getAcl(this.resource));
    }

    /**
     * Check if the request is not deleting, i.e. at most appending to the
     * resource. See N3 patch
     * {@link https://solidproject.org/TR/protocol#writing-resources}.
     *
     * @param method
     * @param body
     * @return isForNonDeletingRequest
     */
    private boolean checkForNonDeletingRequest() {
        switch (this.method) {
            case "GET", "POST" -> {
                // if on resource then not allowed anyway - on ldp:Container it is append.
                return true;
            }
            case "PUT", "DELETE" -> {
                return false;
            }
            case "PATCH" -> {
                String b = this.body;
                // Apache Jena cannot parse the full N3 patch, so we hack it.
                Pattern p = Pattern.compile("\\{[^{}}]*\\}");
                Matcher matcher = p.matcher(b);
                while (matcher.find()) {
                    b = matcher.replaceAll("_:placeholder "); // replace all the blocks just with a blanknode.
                    matcher = p.matcher(b);
                }
                InputStream in = new ByteArrayInputStream(b.getBytes(StandardCharsets.UTF_8));
                Model model = RDFParser.create()
                        .source(in)
                        .lang(RDFLanguages.TRIG)
                        .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                        .toModel();
                String queryString = String.format("""
                                                        PREFIX solid: <%s>
                                                        ASK {
                                                         ?patch solid:deletes ?something
                                                        }
                                                    """, Namespaces.SOLID);

                QueryExecution qexec = QueryExecutionFactory.create(queryString, model);
                boolean deletesSomething = qexec.execAsk();
                return !deletesSomething; // isOnlyAppending
            }
            default ->
                throw new IllegalArgumentException("Unknown HTTP method");
        }
    }

    /**
     * Retrieve the access mode.
     *
     * If acl:Append is retrieved, of course, an acl:Write is also acceptable in
     * a rule.
     *
     * @return the access mode URI string
     */
    private String getAccessMode() {
        if (this.isForControlRequest) {
            return Namespaces.ACL + "Control";
        }
        switch (this.method) {
            case "GET":
                return Namespaces.ACL + "Read";
            case "POST":
            case "PATCH":
                if (this.isForNonDeletingRequest) {
                    // if it is "non deleting / only appending" request then an acl:Append would suffice.
                    return Namespaces.ACL + "Append";
                }
            // if we receive a request, which is NOT "only appending to a resource",
            // i.e. also deletes something, then we need acl:Write
            case "PUT":
            case "DELETE":
                return Namespaces.ACL + "Write";
            default:
                throw new IllegalArgumentException("Unknown HTTP method");
        }
    }

    private void findAuthoritativeACL(String res) {
        if (this.isForControlRequest) {
            res = this.resourceAclMap.getResource(res);
        }
        this.onResource = res;
        String currentAclUriString = this.resourceAclMap.getAcl(res);
        Dataset currentAcl = this.resourceMap.get(currentAclUriString);
        // found .acl ?
        if (currentAcl != null && currentAcl.containsNamedModel(currentAclUriString)) {
            this.authoritativeACL = currentAcl;
            return;
        }
        // > not found.
        this.hasInheritedRule = true;
        // check for directory .acl ...
        String dir = res;
        if (res.endsWith("/")) {
            // is already directory
            dir = dir.substring(0, dir.length() - 1);
            // > omit last slash
        }
        // move one directory up
        int lastIndexOfSlash = dir.lastIndexOf("/");
        // was root directory?
        if (dir.indexOf("://") == lastIndexOfSlash - 2) {
            this.authoritativeACL = DatasetFactory.create(); // nothing found, give back empty root .acl.
            return;
        }
        dir = dir.substring(0, lastIndexOfSlash + 1);
        this.findAuthoritativeACL(dir);
    }

    /**
     * Build all {@link WacQuery} from the information currently in the builder.
     *
     * @return the {@link WacQuery} array of length 1 (if webid is {@code null},
     * no valid authentication assumed) or length 4 (if webid provided, valid
     * authentication assumed)
     */
    public WacQuery[] build() {
        this.findAuthoritativeACL(this.resource); // set authoritativeACL, onResource, and hasInheritedRule
        WacQuery pub = new QueryForPublic(this.authoritativeACL, this.onResource, this.hasInheritedRule, this.accessMode);
        if (this.webid == null) {
            WacQuery[] result = {pub};
            return result;
        }
        WacQuery authn = new QueryForAuthenticated(this.authoritativeACL, this.onResource, this.hasInheritedRule, this.accessMode);
        WacQuery agent = new QueryForAgent(this.authoritativeACL, this.onResource, this.hasInheritedRule, this.accessMode, this.webid);
        WacQuery group = new QueryForAgentGroup(this.authoritativeACL, this.onResource, this.hasInheritedRule, this.accessMode, this.webid, this.resourceMap);
        WacQuery[] result = {pub, authn, agent, group};
        return result;
    }

}
