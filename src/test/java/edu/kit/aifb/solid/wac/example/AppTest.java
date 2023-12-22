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
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.aifb.solid.wac.WacMapping;

/**
 *
 * @author ik1533
 */
public class AppTest {

    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PATCH = "PATCH";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";

    private static Dataset dataset;

    private static String address;
    private static String webid;
    private static String group;
    private static String groupAppendAcl;
    private static String agentControlAcl;
    private static String authenticatedWriteAcl;
    private static String publicReadAcl;
    private static String noAclFound;
    private static String containerWithAcl;

    private static String patchInsertDelete;
    private static String patchInsert;

    private static WacMapping envResourceAclMap;
    private static Map<String, Dataset> envResourceMap;

    private static void addAsNamedGraph(Dataset dataset, String name, String ttl) {
        Model model = dataset.getNamedModel(name);
        InputStream stream = new ByteArrayInputStream(ttl.getBytes(StandardCharsets.UTF_8));
        RDFDataMgr.read(model, stream, name, Lang.TTL);
    }

    /**
     *
     */
    @BeforeClass
    public static void setUp() {
        // Create an RDF dataset by parsing the Turtle data
        dataset = DatasetFactory.create();

        address = "http://example.org/test/";

        webid = address + "webid";

        group = address + "group";
        String groupTTL = String.format(
                """
                    @prefix vcard: <http://www.w3.org/2006/vcard/ns#>.
                    <group#1> vcard:hasMember <%s> .
                """, webid);
        addAsNamedGraph(dataset, group, groupTTL);

        groupAppendAcl = address + "testAgentGroupAccessControl.acl";
        String groupAppendAclTTL = String.format(
                """
                    @prefix acl: <http://www.w3.org/ns/auth/acl#> .
                    <#nohit> a acl:Authorization;
                        acl:agentGroup <group#0>, <other#0>;
                        acl:accessTo <%s>;
                        acl:mode acl:Append.
                    <#auth> a acl:Authorization;
                        acl:agentGroup <other#0>, <group#1>;
                        acl:accessTo <%s>;
                        acl:mode acl:Append.
                """, groupAppendAcl.split(".acl")[0], groupAppendAcl.split(".acl")[0]);
        addAsNamedGraph(dataset, groupAppendAcl, groupAppendAclTTL);

        agentControlAcl = address + "testAgentAccessAppend.acl";
        String agentControlAclTTL = String.format(
                """
                    @prefix acl: <http://www.w3.org/ns/auth/acl#> .
                    <#auth> a acl:Authorization;
                        acl:agent <webid>;
                        acl:accessTo <%s>;
                        acl:mode acl:Control.
                """, agentControlAcl.split(".acl")[0]);
        addAsNamedGraph(dataset, agentControlAcl, agentControlAclTTL);

        authenticatedWriteAcl = address + "testAuthenticatedAccessWrite.acl";
        String authenticatedWriteAclTTL = String.format(
                """
                    @prefix acl: <http://www.w3.org/ns/auth/acl#> .
                    <#auth> a acl:Authorization;
                        acl:agentClass acl:AuthenticatedAgent;
                        acl:accessTo <%s>;
                        acl:mode acl:Write.
                """, authenticatedWriteAcl.split(".acl")[0]);
        addAsNamedGraph(dataset, authenticatedWriteAcl, authenticatedWriteAclTTL);

        publicReadAcl = address + "testPublicAccessRead.acl";
        String publicReadAclTTL = String.format(
                """
                    @prefix acl: <http://www.w3.org/ns/auth/acl#> .
                    @prefix foaf: <http://xmlns.com/foaf/0.1/> .
                    <#auth> a acl:Authorization;
                        acl:agentClass foaf:Agent;
                        acl:accessTo <%s>;
                        acl:mode acl:Read.
                """, publicReadAcl.split(".acl")[0]);
        addAsNamedGraph(dataset, publicReadAcl, publicReadAclTTL);

        containerWithAcl = address + "testInheritedRule/.acl";
        String containerWithAclTTL = String.format(
                """
                    @prefix acl: <http://www.w3.org/ns/auth/acl#> .
                    @prefix foaf: <http://xmlns.com/foaf/0.1/> .
                    <#auth> a acl:Authorization;
                        acl:agentClass foaf:Agent;
                        acl:default <%s>;
                        acl:mode acl:Read.
                """, containerWithAcl.split(".acl")[0]);
        addAsNamedGraph(dataset, containerWithAcl, containerWithAclTTL);

        noAclFound = address + "someContainer/someFile";

        patchInsertDelete = """
                                @prefix solid: <http://www.w3.org/ns/solid/terms#>.
                                @prefix ex: <http://www.example.org/terms#>.
                                _:rename a solid:InsertDeletePatch;
                                  solid:where   { ?person ex:familyName \"Garcia\". };
                                  solid:inserts { ?person ex:givenName \"Alex\". };
                                  solid:deletes { ?person ex:givenName \"Claudia\". }.
                            """;

        patchInsert = """
                            @prefix solid: <http://www.w3.org/ns/solid/terms#>.
                            @prefix ex: <http://www.example.org/terms#>.
                            _:rename a solid:InsertDeletePatch;
                              solid:where   { ?person ex:familyName \"Garcia\". };
                              solid:inserts { ?person ex:givenName \"Alex\". }.
                        """;

        envResourceAclMap = new WacMapping() {
            static final String ACL_SUFFIX = ".acl";

            @Override
            public String getAcl(String resource) {
                String res = resource.split("#")[0];
                return (res.endsWith(ACL_SUFFIX)) ? res : res + ACL_SUFFIX;
            }

            @Override
            public String getResource(String acl) {
                return acl.split(ACL_SUFFIX)[0];
            }
        };
        envResourceMap = new HashMap<String, Dataset>() {
            @Override
            public Dataset get(Object key) {
                if (!(key instanceof String)) {
                    return null;
                }
                return dataset;
            }
        };
    }

    /*
         * PUBLIC ACCESS READ
     */
    @Test
    public void testPublicAccessReadGET() {
        String resource = publicReadAcl.split(".acl")[0];
        String method = GET;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, null, envResourceMap, envResourceAclMap);
        boolean ok = (rule != null) & rule.equals(publicReadAcl + "#auth");
        assertTrue("Expected: rule=<" + publicReadAcl + "#auth>; Result: rule=" + rule, ok);
    }

    @Test
    public void testPublicAccessReadPATCH() {
        String resource = publicReadAcl.split(".acl")[0];
        String method = PATCH;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, null, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testPublicAccessReadPOST() {
        String resource = publicReadAcl.split(".acl")[0];
        String method = POST;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, null, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testPublicAccessReadPUT() {
        String resource = publicReadAcl.split(".acl")[0];
        String method = PUT;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, null, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testPublicAccessReadDELETE() {
        String resource = publicReadAcl.split(".acl")[0];
        String method = DELETE;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, null, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    /*
         * AUTHENTICATED ACCESS WRITE
     */
    @Test
    public void testAuthenticatedAccessWriteUNAUTHENTICATED() {
        String resource = authenticatedWriteAcl.split(".acl")[0];
        String method = DELETE; // random choice
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, null, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testAuthenticatedAccessWriteGET() {
        String resource = authenticatedWriteAcl.split(".acl")[0];
        String method = GET;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testAuthenticatedAccessWritePATCHwithDelete() {
        String resource = authenticatedWriteAcl.split(".acl")[0];
        String method = PATCH;
        String body = patchInsertDelete;
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule != null) & rule.equals(authenticatedWriteAcl + "#auth");
        assertTrue("Expected: rule=<" + authenticatedWriteAcl + "#auth>; Result: rule=" + rule, ok);
    }

    @Test
    public void testAuthenticatedAccessWritePATCHappendOnly() {
        String resource = authenticatedWriteAcl.split(".acl")[0];
        String method = PATCH;
        String body = patchInsert;
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule != null) & rule.equals(authenticatedWriteAcl + "#auth");
        assertTrue("Expected: rule=<" + authenticatedWriteAcl + "#auth>; Result: rule=" + rule, ok);
    }

    @Test
    public void testAuthenticatedAccessWritePOST() {
        String resource = authenticatedWriteAcl.split(".acl")[0];
        String method = POST;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule != null) & rule.equals(authenticatedWriteAcl + "#auth");
        assertTrue("Expected: rule=<" + authenticatedWriteAcl + "#auth>; Result: rule=" + rule, ok);
    }

    @Test
    public void testAuthenticatedAccessWritePUT() {
        String resource = authenticatedWriteAcl.split(".acl")[0];
        String method = PUT;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule != null) & rule.equals(authenticatedWriteAcl + "#auth");
        assertTrue("Expected: rule=<" + authenticatedWriteAcl + "#auth>; Result: rule=" + rule, ok);
    }

    @Test
    public void testAuthenticatedAccessWriteDELETE() {
        String resource = authenticatedWriteAcl.split(".acl")[0];
        String method = DELETE;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule != null) & rule.equals(authenticatedWriteAcl + "#auth");
        assertTrue("Expected: rule=<" + authenticatedWriteAcl + "#auth>; Result: rule=" + rule, ok);
    }

    /*
         * AGENT GROUP ACCESS APPEND
     */
    @Test
    public void testGroupAccessAppendUNAUTHENTICATED() {
        String resource = groupAppendAcl.split(".acl")[0];
        String method = PATCH; // random choice
        String body = patchInsertDelete;
        String rule = App.checkAccessControl(resource, method, body, null, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testGroupAccessAppendGET() {
        String resource = groupAppendAcl.split(".acl")[0];
        String method = GET;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testGroupAccessAppendPATCHwithDelete() {
        String resource = groupAppendAcl.split(".acl")[0];
        String method = PATCH;
        String body = patchInsertDelete;
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testGroupAccessAppendPATCHappendOnly() {
        String resource = groupAppendAcl.split(".acl")[0];
        String method = PATCH;
        String body = patchInsert;
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule != null) & rule.equals(groupAppendAcl + "#auth");
        assertTrue("Expected: rule=<" + groupAppendAcl + "#auth>; Result: rule=" + rule, ok);
    }

    @Test
    public void testGroupAccessAppendPOST() {
        String resource = groupAppendAcl.split(".acl")[0];
        String method = POST;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule != null) & rule.equals(groupAppendAcl + "#auth");
        assertTrue("Expected: rule=<" + groupAppendAcl + "#auth>; Result: rule=" + rule, ok);
    }

    @Test
    public void testGroupAccessAppendPUT() {
        String resource = groupAppendAcl.split(".acl")[0];
        String method = PUT;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testGroupAccessAppendDELETE() {
        String resource = groupAppendAcl.split(".acl")[0];
        String method = DELETE;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    /*
         * AGENT ACCESS CONTROL
     */
    @Test
    public void testAgentAccessControlUNAUTHENTICATED() {
        String resource = agentControlAcl;
        String method = PUT; // random choice
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, null, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testAgentAccessControlGET() {
        String resource = agentControlAcl;
        String method = GET;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule != null) & rule.equals(agentControlAcl + "#auth");
        assertTrue("Expected: rule=<" + agentControlAcl + "#auth>; Result: rule=" + rule, ok);
    }

    @Test
    public void testAgentAccessControlPATCHwithDelete() {
        String resource = agentControlAcl;
        String method = PATCH;
        String body = patchInsertDelete;
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule != null) & rule.equals(agentControlAcl + "#auth");
        assertTrue("Expected: rule=<" + agentControlAcl + "#auth>; Result: rule=" + rule, ok);
    }

    @Test
    public void testAgentAccessControlPATCHappendOnly() {
        String resource = agentControlAcl;
        String method = PATCH;
        String body = patchInsert;
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule != null) & rule.equals(agentControlAcl + "#auth");
        assertTrue("Expected: rule=<" + agentControlAcl + "#auth>; Result: rule=" + rule, ok);
    }

    @Test
    public void testAgentAccessControlPOST() {
        String resource = agentControlAcl;
        String method = POST;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule != null) & rule.equals(agentControlAcl + "#auth");
        assertTrue("Expected: rule=<" + agentControlAcl + "#auth>; Result: rule=" + rule, ok);
    }

    @Test
    public void testAgentAccessControlPUT() {
        String resource = agentControlAcl;
        String method = PUT;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule != null) & rule.equals(agentControlAcl + "#auth");
        assertTrue("Expected: rule=<" + agentControlAcl + "#auth>; Result: rule=" + rule, ok);
    }

    @Test
    public void testAgentAccessControlDELETE() {
        String resource = agentControlAcl;
        String method = DELETE;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule != null) & rule.equals(agentControlAcl + "#auth");
        assertTrue("Expected: rule=<" + agentControlAcl + "#auth>; Result: rule=" + rule, ok);
    }

    // check resource access, there should be none since only control rights!
    @Test
    public void testAgentAccessControlUNAUTHENTICATEDrescoure() {
        String resource = agentControlAcl.split(".acl")[0];
        String method = GET; // random choice
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, null, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testAgentAccessControlGETresource() {
        String resource = agentControlAcl.split(".acl")[0];
        String method = GET;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testAgentAccessControlPATCHwithDeleteResource() {
        String resource = agentControlAcl.split(".acl")[0];
        String method = PATCH;
        String body = patchInsertDelete;
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testAgentAccessControlPATCHappendOnlyResource() {
        String resource = agentControlAcl.split(".acl")[0];
        String method = PATCH;
        String body = patchInsert;
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testAgentAccessControlPOSTresource() {
        String resource = agentControlAcl.split(".acl")[0];
        String method = POST;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testAgentAccessControlPUTresource() {
        String resource = agentControlAcl.split(".acl")[0];
        String method = PUT;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testAgentAccessControlDELETEresource() {
        String resource = agentControlAcl.split(".acl")[0];
        String method = DELETE;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    /*
         * NO ACL FOUND
     */
    @Test
    public void testAccessControlUNAUTHENTICATEDNotFound() {
        String resource = noAclFound.split(".acl")[0];
        String method = PUT; // random choice
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, null, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testAccessControlNotFoundGET() {
        String resource = noAclFound.split(".acl")[0];
        String method = GET;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testAccessControlNotFoundDelete() {
        String resource = noAclFound.split(".acl")[0];
        String method = DELETE;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testAccessControlNotFoundndAppendOnly() {
        String resource = noAclFound.split(".acl")[0];
        String method = PATCH;
        String body = patchInsert;
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testAccessControlNotFoundolPOST() {
        String resource = noAclFound.split(".acl")[0];
        String method = POST;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testAccessControlNotFoundPUT() {
        String resource = noAclFound.split(".acl")[0];
        String method = PUT;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    @Test
    public void testAccessControlNotFoundDELETE() {
        String resource = noAclFound.split(".acl")[0];
        String method = DELETE;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, webid, envResourceMap, envResourceAclMap);
        boolean ok = (rule == null);
        assertTrue("Expected: rule=null; Result: rule=" + rule, ok);
    }

    /*
         * INHERITED ACL FOUND
     */
    @Test
    public void testAccessControlUNAUTHENTICATEDInheritedPublicRead() {
        String resource = containerWithAcl.split(".acl")[0] + "someContainer/someFile";
        String method = GET;
        String body = "";
        String rule = App.checkAccessControl(resource, method, body, null, envResourceMap, envResourceAclMap);
        boolean ok = (rule != null) & rule.equals(containerWithAcl + "#auth");
        assertTrue("Expected: rule=<" + containerWithAcl + "#auth>; Result: rule=" + rule, ok);
    }
}
