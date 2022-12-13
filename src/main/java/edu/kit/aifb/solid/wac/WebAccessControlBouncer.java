package edu.kit.aifb.solid.wac;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.ErrorHandlerFactory;

import edu.kit.aifb.solid.wac.exception.SolidWacException;
import edu.kit.aifb.solid.wac.query.QueryType;
import edu.kit.aifb.solid.wac.query.WebQueryBuilder;

public class WebAccessControlBouncer {

    static final String ACL_SUFFIX = ".acl";

    /**
     * Check the rules!
     * If webid == null, then the request is assumed to be unauthenticated.
     * If webid != null, then the request is assumed to be authenticated.
     * 
     * @param resource the target of the HTTP request of an agent
     * @param method   the HTTP method to be executed on the target resource
     * @param body     MUST not be null, for PATCH the body should always be
     *                 provided, otherwise shouldnt
     * @param webid    if != null, it is assumed to be authenticated
     * @return true if there is no exception, otherwise throw a
     *         {@link SolidWacException}
     * @throws SolidWacException 401 if unauthenticated, 403 if unauthorized, 500 if
     *                           server error
     */
    public static boolean checkAccessControl(String resource, String method, String body, String webid)
            throws SolidWacException {
        // make sure the resource has no fragment
        resource = resource.split("#")[0];
        // get the query builder for the resource (may look for inherited rules)
        WebQueryBuilder queryBuilder = getQueryBuilder(resource);
        queryBuilder.method(method);
        if (isOnlyAppendingRequest(method, body)) {
            // check if append suffices or not
            queryBuilder.isOnlyAppending();
        }
        // check if resource is public
        boolean isPublic = queryBuilder.build(QueryType.QUERY_FOR_PUBLIC).execOnWeb();
        if (isPublic) { // public -> 200
            return true;
        }
        // authentication required -> assuming if webid != null then it is authn
        if (webid == null) { // not public and not authenticated -> 401
            throw new SolidWacException(401, "Unauthenticated");
        }
        // set authenticated webid
        queryBuilder.webid(webid);
        // build all the queries
        QueryType[] queries = {
                QueryType.QUERY_FOR_AUTHENTICATED,
                QueryType.QUERY_FOR_AGENT,
                QueryType.QUERY_FOR_AGENTGROUP
        };
        for (QueryType query : queries) {
            // check in sequence if query matches
            boolean hasMatch = queryBuilder.build(query).execOnWeb();
            if (hasMatch) {
                return true;
            }
        }
        // if all false -> 403
        throw new SolidWacException(403, "Unauthorized");
    }

    /**
     * Recursively retrieve the query builder for the applicable {@code .acl}.
     * 
     * @param resource
     * @return {@link WebQueryBuilder}
     * @throws SolidWacException
     */
    private static WebQueryBuilder getQueryBuilder(String resource) throws SolidWacException {
        String acl = findCorrespondingAclResource(resource);
        try {
            return WebQueryBuilder.newBuilder()
                    .acl(acl)
                    .resource(resource);
        } catch (HttpException e) {
            switch (e.getStatusCode()) {
                case 404: // .acl does not exist
                    String res = resource;
                    if (res.endsWith("/")) {
                        res = res.substring(0, resource.length() - 1);
                    }
                    int lastIndexOfSlash = res.lastIndexOf("/");
                    if (resource.indexOf("://") == lastIndexOfSlash - 2) {
                        throw new SolidWacException(500, "No ACL found at root directory");
                    }
                    res = res.substring(0, lastIndexOfSlash + 1);
                    return getQueryBuilder(res);
                default: // something is wrong, e.g. no access
                    throw new SolidWacException(500, e.getMessage());
            }
        }
    }

    /**
     * Check if the request is only appending to the resource.
     * See N3 patch {@link https://solidproject.org/TR/protocol#writing-resources}.
     * 
     * @param method
     * @param body
     * @return isOnlyAppending
     */
    private static boolean isOnlyAppendingRequest(String method, String body) {
        switch (method) {
            case "GET":
            case "POST": // idk?
                return true;
            case "PUT":
            case "DELETE":
                return false;
            case "PATCH":
                String b = body;
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
            default:
                throw new IllegalArgumentException("Unknown HTTP method");
        }
    }

    /**
     * Retrieves the {@code .acl} for a resource derived by convention:
     * resource.acl
     * Override this if you want to look up such mapping from a database.
     * 
     * @param resource
     * 
     * @return resource.acl or if it is already an acl, return itself
     */
    public static String findCorrespondingAclResource(String resource) {
        String res = resource.split("#")[0];
        if (res.endsWith(ACL_SUFFIX)) {
            return res;
        }
        return res + ACL_SUFFIX;
    }

    /**
     * Retrieves the resource for an {@code .acl} derived by convention:
     * resource.acl
     * Override this if you want to look up such mapping from a database.
     * 
     * @param acl
     * 
     * @return resource or if it is not an acl, return itself
     */
    public static String findCorrespondingResource(String acl) {
        return acl.split(ACL_SUFFIX)[0];
    }

}
