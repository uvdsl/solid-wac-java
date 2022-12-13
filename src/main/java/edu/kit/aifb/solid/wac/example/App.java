package edu.kit.aifb.solid.wac.example;

import edu.kit.aifb.solid.wac.WebAccessControlBouncer;
import edu.kit.aifb.solid.wac.exception.SolidWacException;

/**
 * Hello world!
 *
 */
public class App {

        /**
         * example usage
         */
        public static void main(String[] args) {
                // mock a HTTP request
                String resource = "http://localhost:8080/marmotta/ldp/test.acl#123";
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
                boolean isGrantingAccess = false;
                try {
                        isGrantingAccess = WebAccessControlBouncer
                                        .checkAccessControl(resource, method, body, webid);
                } catch (SolidWacException e) {
                        System.out.println(e.getStatusCode() + " - " + e.getMessage());
                }
                // result
                System.out.println("Access granted: " + isGrantingAccess);
                // happy hacking
                System.exit(0);
        }
}
