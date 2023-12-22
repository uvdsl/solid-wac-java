package edu.kit.aifb.solid.wac.example;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import edu.kit.aifb.solid.wac.WacMapping;
import edu.kit.aifb.solid.wac.query.WacQuery;
import edu.kit.aifb.solid.wac.query.WacQueryBuilder;

/**
 * Example usage
 *
 */
public class App {

    /**
     * Check the rules! If webid == null, then the request is assumed to be
     * unauthenticated. If webid != null, then the request is assumed to be
     * authenticated.
     *
     * @param resource the target of the HTTP request of an agent
     * @param method the HTTP action to be executed on the target resource
     * @param body MUST not be null, for PATCH the body should always be
     * provided, otherwise shouldnt
     * @param webid if != null, it is assumed to be authenticated
     * @param envResourceMap appplication environment resource map
     * @param envResourceAclMap application environement resource to
     * corresponding aclRDF map
     * @return the URI String of the matching access control rule or
     * {@code null} if none matches
     *
     */
    public static String checkAccessControl(String resource, String method, String body, String webid, Map<String, Dataset> envResourceMap, WacMapping envResourceAclMap) {
        // get the query builder for the resource (may look for inherited rules)
        WacQueryBuilder queryBuilder = WacQueryBuilder
                .newBuilder(envResourceMap, envResourceAclMap)
                .forRequest(resource, method, body)
                .byAgent(webid);
        WacQuery[] queries = queryBuilder.build();
        for (WacQuery query : queries) {
            String ruleMatch = query.exec();
            if (ruleMatch == null) {
                continue;
            }
            return ruleMatch;
        }
        return null;
    }

    /**
     * example usage
     *
     * @param args
     */
    public static void main(String[] args) {
        // mock application environment
        WacMapping envResourceAclMap = new ResourceAclMap(); // ! IMPLEMENTATION DEPENDENT 
        Map<String, Dataset> envResourceMap = new HashMap(); // ! IMPLEMENTATION DEPENDENT 
        String resource = "http://localhost:8080/marmotta/ldp/test";
        String acl = envResourceAclMap.getAcl(resource);
        System.out.println("ACL: " + acl);
        String someAcl = """
                            @prefix acl: <http://www.w3.org/ns/auth/acl#> 
                            <#testAgentGroupAccess> a acl:Authorization;
                                acl:agent <http://example.org/webid>;
                                acl:accessTo <test>;
                                acl:mode acl:Write.
        """;
        // Create an RDF dataset by parsing the Turtle data
        Dataset dataset = DatasetFactory.create();
        Model aclRDF = dataset.getNamedModel(acl);
        InputStream stream = new ByteArrayInputStream(someAcl.getBytes(StandardCharsets.UTF_8));
        RDFDataMgr.read(aclRDF, stream, acl, Lang.TTL);
        // Access the dataset
        System.out.println("RDF Dataset:");
        dataset.getNamedModel(acl).write(System.out, "TTL");  // Example: Turtle format
        envResourceMap.put(acl, dataset);
        // mock a HTTP request
        String webid = "http://example.org/webid";
        String method = "PATCH";
        String body = """
                                @prefix solid: <http://www.w3.org/ns/solid/terms#>.
                                @prefix ex: <http://www.example.org/terms#>.

                                _:rename a solid:InsertDeletePatch;
                                  solid:where   { ?person ex:familyName \"Garcia\". };
                                  solid:inserts { ?person ex:givenName \"Alex\". };
                                  solid:deletes { ?person ex:givenName \"Claudia\". }.
                                """;
        // check WAC rules
        String ruleGrantingAccess = checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        // result
        System.out.println("\nAccess granted? " + ((ruleGrantingAccess == null) ? "No." : "Yes: " + ruleGrantingAccess));
        // happy hacking
        System.exit(0);
    }
}

/**
 * ! IMPLEMENTATION DEPENDENT mapping of resource and .aclRDF
 *
 * Retrieves the {@code .aclRDF} for a resource derived by convention:
 * resource.aclRDF Override this if you want to look up such mapping from a
 * database.
 *
 */
class ResourceAclMap implements WacMapping {

    static final String ACL_SUFFIX = ".acl";

    @Override
    public String getAcl(String resource) {
        String res = resource.split("#")[0];
        return (res.endsWith(ACL_SUFFIX)) ? res : res + ACL_SUFFIX;
    }

    @Override
    public String getResource(String acl) {
        return acl.split("#")[0];
    }

}
