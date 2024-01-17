# Solid Web Access Control

This library implements checking Web Access Control rules in plain Java, (... _sips coffee_ ...).

Software using this code needs to

- define the bi-directional mapping between a `resource` and its corresponding `.acl` using the interface [Example](src/main/java/edu/kit/aifb/solid/wac/WacMapping.java)
- provide a map between a `resource` URI string and an its corresponding RDF dataset as a `Map<String, Dataset>`.

Why is the `resource`-`.acl` mapping bi-directional? Why can't we use one `.acl` to rule them all?
`acl:Control` allows modification of access control rules which have the same `acl:target` as the control rule.
Allowing multiple rules with different targets, i.e. allowing one `.acl` to be authoritative for multiple target resources makes enforcing the control right unnecessarily complicated: You can't allow HTTP PUT or DELETE, if there exist rules for other resources, and you have to do in-depth checks of HTTP PATCH to not modify rules for other resources.

You _can_ do this. But I chose not to.

For `.acl` checking using the (resource - RDF dataset) map, we check if the `.acl` dataset is present in the map.
We then assume that the triples describing access control rules are placed in the default graph.

## [Example](src/main/java/edu/kit/aifb/solid/wac/example/) usage:

```java
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
```
By the way, WAC does not really define behaviour for the HTTP method OPTIONS. 
OPTIONS is common for CORS pre-flight requests.
Be sure to hanlde OPTIONS manually. 
Otherwise you may keep wondering why your code does not work.

Want to run an example directly? Naybe something like:
```java
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
        Model aclRDF = dataset.getDefaultModel();
        InputStream stream = new ByteArrayInputStream(someAcl.getBytes(StandardCharsets.UTF_8));
        RDFDataMgr.read(aclRDF, stream, acl, Lang.TTL);
        // Access the dataset
        System.out.println("RDF Dataset:");
        dataset.getDefaultModel().write(System.out, "TTL");  // Example: Turtle format
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
```
Sure, after cloning this repository, with Maven:
```
mvn package
mvn exec:java
```

## Tests?

Sure, after cloning this repository, with Maven:

```
mvn package
```

Then, find the test coverage report at `target/site/jacoco/index.html`.

## Dependencies

We rely on [Apache Jena](https://mvnrepository.com/artifact/org.apache.jena/jena-core) for handling the RDF.
