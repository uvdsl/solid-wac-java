package edu.kit.aifb.solid.wac;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;

import edu.kit.aifb.solid.wac.exception.SolidWacException;

public class WebAccessControlBouncerTest {

        private static String GET = "GET";
        private static String POST = "POST";
        private static String PATCH = "PATCH";
        private static String PUT = "PUT";
        private static String DELETE = "DELETE";

        private static boolean isSetup = false;
        private static ClientAndServer mockServer;
        private static String address;
        private static String webid;
        private static String groupPath;
        private static String groupAppendAclPath;
        private static String agentControlAclPath;
        private static String authenticatedWriteAclPath;
        private static String publicReadAclPath;
        private static String noAclFoundPath;

        private static String patchInsertDelete;
        private static String patchInsert;

        @BeforeClass
        public static void setUp() {
                if (isSetup) {
                        return;
                }
                int port = 9999;
                if (mockServer == null) {
                        mockServer = startClientAndServer(port);
                        address = "http://" + mockServer.remoteAddress().getHostName() + ":" + port;
                        webid = address + "/webid";
                        groupPath = "/group";
                        groupAppendAclPath = "/testAgentGroupAccessControl.acl";
                        agentControlAclPath = "/testAgentAccessAppend.acl";
                        authenticatedWriteAclPath = "/testAuthenticatedAccessWrite.acl";
                        publicReadAclPath = "/testPublicAccessRead.acl";
                        noAclFoundPath = "/someContainer/someFile";

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

                        mockServer.when(
                                        request()
                                                        .withPath("/.acl"))
                                        .respond(
                                                        response().withStatusCode(401));
                        mockServer.when(
                                        request()
                                                        .withPath(groupPath))
                                        .respond(
                                                        response()
                                                                        .withContentType(
                                                                                        new MediaType("text", "turtle"))
                                                                        .withBody(String.format(
                                                                                        """
                                                                                                        @prefix vcard: <http://www.w3.org/2006/vcard/ns#>.
                                                                                                        <group#1> vcard:hasMember <webid> .
                                                                                                            """)));
                        mockServer.when(
                                        request()
                                                        .withPath(groupAppendAclPath))
                                        .respond(
                                                        response()
                                                                        .withContentType(
                                                                                        new MediaType("text", "turtle"))
                                                                        .withBody(String.format(
                                                                                        """
                                                                                                        @prefix acl: <http://www.w3.org/ns/auth/acl#> .

                                                                                                        <testAgentGroupAccess#auth> a acl:Authorization;
                                                                                                            acl:agentGroup <group#0>, <other#0>, <group#1>;
                                                                                                            acl:accessTo <%s>;
                                                                                                            acl:mode acl:Append.
                                                                                                            """,
                                                                                        groupAppendAclPath.split(
                                                                                                        ".acl")[0])));
                        mockServer.when(
                                        request()
                                                        .withPath(agentControlAclPath))
                                        .respond(
                                                        response()
                                                                        .withContentType(
                                                                                        new MediaType("text", "turtle"))
                                                                        .withBody(String.format(
                                                                                        """
                                                                                                        @prefix acl: <http://www.w3.org/ns/auth/acl#> .

                                                                                                        <testAgentAccess#auth> a acl:Authorization;
                                                                                                            acl:agent <webid>;
                                                                                                            acl:accessTo <%s>;
                                                                                                            acl:mode acl:Control.
                                                                                                            """,
                                                                                        agentControlAclPath.split(
                                                                                                        ".acl")[0])));
                        mockServer.when(
                                        request()
                                                        .withPath(authenticatedWriteAclPath))
                                        .respond(
                                                        response()
                                                                        .withContentType(
                                                                                        new MediaType("text", "turtle"))
                                                                        .withBody(String.format(
                                                                                        """
                                                                                                        @prefix acl: <http://www.w3.org/ns/auth/acl#> .

                                                                                                        <testAuthenticatedAccess#auth> a acl:Authorization;
                                                                                                            acl:agentClass acl:AuthenticatedAgent;
                                                                                                            acl:accessTo <%s>;
                                                                                                            acl:mode acl:Write.
                                                                                                            """,
                                                                                        authenticatedWriteAclPath.split(
                                                                                                        ".acl")[0])));
                        mockServer.when(
                                        request()
                                                        .withPath(publicReadAclPath))
                                        .respond(
                                                        response()
                                                                        .withContentType(
                                                                                        new MediaType("text", "turtle"))
                                                                        .withBody(String.format(
                                                                                        """
                                                                                                        @prefix acl: <http://www.w3.org/ns/auth/acl#> .
                                                                                                        @prefix foaf: <http://xmlns.com/foaf/0.1/> .

                                                                                                        <testPublicAccess#auth> a acl:Authorization;
                                                                                                            acl:agentClass foaf:Agent;
                                                                                                            acl:accessTo <%s>;
                                                                                                            acl:mode acl:Read.
                                                                                                            """,
                                                                                        publicReadAclPath.split(
                                                                                                        ".acl")[0])));
                }
                isSetup = true;
        }

        @AfterClass
        public static void tearDown() {
                mockServer.stop();
        }

        /*
         * PUBLIC ACCESS READ
         */

        @Test
        public void testPublicAccessReadGET() throws IOException, InterruptedException {
                String targetResource = (address + publicReadAclPath).split(".acl")[0];
                boolean ok = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, GET, "", null);
                } catch (SolidWacException e) {
                        fail(e.getMessage());
                }
                assertTrue("Expected: ok=true; Result: ok=false", ok);
        }

        @Test
        public void testPublicAccessReadPATCH() throws IOException, InterruptedException {
                String targetResource = (address + publicReadAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PATCH, "", null);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 401) {
                                fail("Expected 401 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testPublicAccessReadPOST() throws IOException, InterruptedException {
                String targetResource = (address + publicReadAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, POST, "", null);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 401) {
                                fail("Expected 401 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testPublicAccessReadPUT() throws IOException, InterruptedException {
                String targetResource = (address + publicReadAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PUT, "", null);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 401) {
                                fail("Expected 401 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testPublicAccessReadDELETE() throws IOException, InterruptedException {
                String targetResource = (address + publicReadAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, DELETE, "", null);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 401) {
                                fail("Expected 401 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        /*
         * AUTHENTICATED ACCESS WRITE
         */

        @Test
        public void testAuthenticatedAccessWriteUNAUTHENTICATED() throws IOException, InterruptedException {
                String targetResource = (address + authenticatedWriteAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PUT, "", null);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 401) {
                                fail("Expected 401 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testAuthenticatedAccessWriteGET() throws IOException, InterruptedException {
                String targetResource = (address + authenticatedWriteAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, GET, "", webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 403) {
                                fail("Expected 403 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testAuthenticatedAccessWritePATCHwithDelete() throws IOException, InterruptedException {
                String targetResource = (address + authenticatedWriteAclPath).split(".acl")[0];
                boolean ok = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PATCH, patchInsertDelete,
                                        webid);
                } catch (SolidWacException e) {
                        fail(e.getMessage());
                }
                assertTrue("Expected: ok=true; Result: ok=false", ok);
        }

        @Test
        public void testAuthenticatedAccessWritePATCHappendOnly() throws IOException, InterruptedException {
                String targetResource = (address + authenticatedWriteAclPath).split(".acl")[0];
                boolean ok = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PATCH, patchInsert, webid);
                } catch (SolidWacException e) {
                        fail(e.getMessage());
                }
                assertTrue("Expected: ok=true; Result: ok=false", ok);
        }

        @Test
        public void testAuthenticatedAccessWritePOST() throws IOException, InterruptedException {
                String targetResource = (address + authenticatedWriteAclPath).split(".acl")[0];
                boolean ok = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, POST, "", webid);
                } catch (SolidWacException e) {
                        fail(e.getMessage());
                }
                assertTrue("Expected: ok=true; Result: ok=false", ok);
        }

        @Test
        public void testAuthenticatedAccessWritePUT() throws IOException, InterruptedException {
                String targetResource = (address + authenticatedWriteAclPath).split(".acl")[0];
                boolean ok = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PUT, "", webid);
                } catch (SolidWacException e) {
                        fail(e.getMessage());
                }
                assertTrue("Expected: ok=true; Result: ok=false", ok);
        }

        @Test
        public void testAuthenticatedAccessWriteDELETE() throws IOException, InterruptedException {
                String targetResource = (address + authenticatedWriteAclPath).split(".acl")[0];
                boolean ok = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PUT, "", webid);
                } catch (SolidWacException e) {
                        fail(e.getMessage());
                }
                assertTrue("Expected: ok=true; Result: ok=false", ok);
        }

        /*
         * AGENT GROUP ACCESS APPEND
         */

        @Test
        public void testGroupAccessAppendUNAUTHENTICATED() throws IOException, InterruptedException {
                String targetResource = (address + groupAppendAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PUT, "", null);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 401) {
                                fail("Expected 401 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testGroupAccessAppendGET() throws IOException, InterruptedException {
                String targetResource = (address + groupAppendAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, GET, "", webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 403) {
                                fail("Expected 403 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testGroupAccessAppendPATCHwithDelete() throws IOException, InterruptedException {
                String targetResource = (address + groupAppendAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PATCH, patchInsertDelete,
                                        webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 403) {
                                fail("Expected 403 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testGroupAccessAppendPATCHappendOnly() throws IOException, InterruptedException {
                String targetResource = (address + groupAppendAclPath).split(".acl")[0];
                boolean ok = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PATCH, patchInsert, webid);
                } catch (SolidWacException e) {
                        fail(e.getMessage());
                }
                assertTrue("Expected: ok=true; Result: ok=false", ok);
        }

        @Test
        public void testGroupAccessAppendPOST() throws IOException, InterruptedException {
                String targetResource = (address + groupAppendAclPath).split(".acl")[0];
                boolean ok = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, POST, "", webid);
                } catch (SolidWacException e) {
                        fail(e.getMessage());
                }
                assertTrue("Expected: ok=true; Result: ok=false", ok);
        }

        @Test
        public void testGroupAccessAppendPUT() throws IOException, InterruptedException {
                String targetResource = (address + groupAppendAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PUT, "", webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 403) {
                                fail("Expected 403 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testGroupAccessAppendDELETE() throws IOException, InterruptedException {
                String targetResource = (address + groupAppendAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, DELETE, "", webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 403) {
                                fail("Expected 403 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        /*
         * AGENT ACCESS CONTROL
         */

        @Test
        public void testAgentAccessControlUNAUTHENTICATED() throws IOException, InterruptedException {
                String targetResource = (address + agentControlAclPath);
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PUT, "", null);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 401) {
                                fail("Expected 401 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testAgentAccessControlGET() throws IOException, InterruptedException {
                String targetResource = (address + agentControlAclPath);
                boolean ok = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, GET, "", webid);
                } catch (SolidWacException e) {
                        fail(e.getMessage());
                }
                assertTrue("Expected: ok=true; Result: ok=false", ok);
        }

        @Test
        public void testAgentAccessControlPATCHwithDelete() throws IOException, InterruptedException {
                String targetResource = (address + agentControlAclPath);
                boolean ok = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PATCH, patchInsertDelete,
                                        webid);
                } catch (SolidWacException e) {
                        fail(e.getMessage());
                }
                assertTrue("Expected: ok=true; Result: ok=false", ok);
        }

        @Test
        public void testAgentAccessControlPATCHappendOnly() throws IOException, InterruptedException {
                String targetResource = (address + agentControlAclPath);
                boolean ok = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PATCH, patchInsert, webid);
                } catch (SolidWacException e) {
                        fail(e.getMessage());
                }
                assertTrue("Expected: ok=true; Result: ok=false", ok);
        }

        @Test
        public void testAgentAccessControlPOST() throws IOException, InterruptedException {
                String targetResource = (address + agentControlAclPath);
                boolean ok = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, POST, "", webid);
                } catch (SolidWacException e) {
                        fail(e.getMessage());
                }
                assertTrue("Expected: ok=true; Result: ok=false", ok);
        }

        @Test
        public void testAgentAccessControlPUT() throws IOException, InterruptedException {
                String targetResource = (address + agentControlAclPath);
                boolean ok = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PUT, "", webid);
                } catch (SolidWacException e) {
                        fail(e.getMessage());
                }
                assertTrue("Expected: ok=true; Result: ok=false", ok);
        }

        @Test
        public void testAgentAccessControlDELETE() throws IOException, InterruptedException {
                String targetResource = (address + agentControlAclPath);
                boolean ok = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, DELETE, "", webid);
                } catch (SolidWacException e) {
                        fail(e.getMessage());
                }
                assertTrue("Expected: ok=true; Result: ok=false", ok);
        }

        // check resource access, there should be none since only control rights!

        @Test
        public void testAgentAccessControlUNAUTHENTICATEDrescoure() throws IOException, InterruptedException {
                String targetResource = (address + agentControlAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PUT, "", null);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 401) {
                                fail("Expected 401 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testAgentAccessControlGETresource() throws IOException, InterruptedException {
                String targetResource = (address + agentControlAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, GET, "", webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 403) {
                                fail("Expected 403 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testAgentAccessControlPATCHwithDeleteResource() throws IOException, InterruptedException {
                String targetResource = (address + agentControlAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PATCH, patchInsertDelete,
                                        webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 403) {
                                fail("Expected 403 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testAgentAccessControlPATCHappendOnlyResource() throws IOException, InterruptedException {
                String targetResource = (address + agentControlAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PATCH, patchInsert, webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 403) {
                                fail("Expected 403 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testAgentAccessControlPOSTresource() throws IOException, InterruptedException {
                String targetResource = (address + agentControlAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, POST, "", webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 403) {
                                fail("Expected 403 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testAgentAccessControlPUTresource() throws IOException, InterruptedException {
                String targetResource = (address + agentControlAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PUT, "", webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 403) {
                                fail("Expected 403 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testAgentAccessControlDELETEresource() throws IOException, InterruptedException {
                String targetResource = (address + agentControlAclPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, DELETE, "", webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 403) {
                                fail("Expected 403 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        /*
         * NO ACL FOUND
         */

        @Test
        public void testAccessControlUNNotFoundICATED() throws IOException, InterruptedException {
                String targetResource = (address + noAclFoundPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PUT, "", null);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 500) {
                                fail("Expected 500 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testAccNotFoundrolGET() throws IOException, InterruptedException {
                String targetResource = (address + noAclFoundPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, GET, "", webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 500) {
                                fail("Expected 500 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testAccessControlPANotFoundDelete() throws IOException, InterruptedException {
                String targetResource = (address + noAclFoundPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PATCH, patchInsertDelete,
                                        webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 500) {
                                fail("Expected 500 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testAccessControlPANotFoundndOnly() throws IOException, InterruptedException {
                String targetResource = (address + noAclFoundPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PATCH, patchInsert, webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 500) {
                                fail("Expected 500 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testAcceNotFoundolPOST() throws IOException, InterruptedException {
                String targetResource = (address + noAclFoundPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, POST, "", webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 500) {
                                fail("Expected 500 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testAccNotFoundrolPUT() throws IOException, InterruptedException {
                String targetResource = (address + noAclFoundPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, PUT, "", webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 500) {
                                fail("Expected 500 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }

        @Test
        public void testAccessNotFoundDELETE() throws IOException, InterruptedException {
                String targetResource = (address + noAclFoundPath).split(".acl")[0];
                boolean ok = false;
                boolean err = false;
                try {
                        ok = WebAccessControlBouncer.checkAccessControl(targetResource, DELETE, "", webid);
                } catch (SolidWacException e) {
                        if (e.getStatusCode() != 500) {
                                fail("Expected 500 but got " + e.getStatusCode() + " as status code");
                        }
                        err = true;
                }
                assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
        }
}
