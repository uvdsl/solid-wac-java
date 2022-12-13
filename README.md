# SOLID Web Access Control

This library implements checking Web Access Control rules in plain Java, (... _sips coffee_ ...).

## [Example](src/main/java/edu/kit/aifb/solid/wac/example/) usage:
```java
        // mock details of a HTTP request
        String resource = "http://example.org/resource";
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
```
Want to run the example directly?
You have to modify the HTTP request details, i.e. `resource` and `webid` in any case and if you want play around with the others.
On your server, provide the corresponding `.acl` resource that you want to test.
Then, after cloning this repository, with Maven:
```
mvn package
mvn exec:java
```
Maybe it is easier to just look at the tests...?

## Tests? 
Sure, after cloning this repository, with Maven:
```
mvn package
```
Then, find the test coverage report at `target/site/jacoco/index.html`.


## Dependencies
We rely on [Apache Jena](https://mvnrepository.com/artifact/org.apache.jena/jena-core) for handling the RDF.