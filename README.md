# SOLID Web Access Control

This library implements handling Web Access Control rules in plain Java, (... _sips coffee_ ...).

The [WebAccessControlBouncer](src/main/java/edu/kit/aifb/solid/wac/WebAccessControlBouncer.java) implements basic functionality which you may want to extend/override. 
In particular, the [WebAccessControlBouncer](src/main/java/edu/kit/aifb/solid/wac/WebAccessControlBouncer.java) determines the mapping between the resource and the corresponding `.acl` resource it will check for rules.
Here, we chose the simple and standard way of simply appending `.acl` to the resource's URI (if not already ending on `.acl`.)
You can extend the [WebAccessControlBouncer](src/main/java/edu/kit/aifb/solid/wac/WebAccessControlBouncer.java) to define your own mapping.

When checking Access Control Rules, the [WebAccessControlBouncer](src/main/java/edu/kit/aifb/solid/wac/WebAccessControlBouncer.java) will always return `true` if any matching rule was found, or throw a [SolidWacExcpetion](src/main/java/edu/kit/aifb/solid/wac/exception/SolidWacException.java) with a status code (401 for unauthenticated, 403 for unauthorized or 500 for another error, most likely because a Web resource could not be retrieved).

Also, when a `webid` is supplied to the [WebAccessControlBouncer](src/main/java/edu/kit/aifb/solid/wac/WebAccessControlBouncer.java), it assumes this `webid` to already be authenticated.
Of course, you can override this behaviour if you want.


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