package edu.kit.aifb.solid.wac.query;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * A builder-pattern for ACL Queries ({@link QueryType}).
 */
public class WebQueryBuilder {

  /**
   * 
   * @return a new builder
   */
  public static WebQueryBuilder newBuilder() {
    return new WebQueryBuilder();
  }

  private WebQueryBuilder() {
  }

  private Dataset dataset;
  private String resource;
  private String acl;
  private String method;
  private boolean isOnlyAppending;
  private String webid;

  /**
   * Set the resource to access.
   * 
   * @param resource
   * @return the builder
   */
  public WebQueryBuilder resource(String resource) {
    this.resource = resource;
    return this;
  }

  /**
   * Set the corresponding acl resource.
   * The builder will directly look the resource up on the Web.
   * 
   * @param acl
   * @return the builder
   */
  public WebQueryBuilder acl(String acl) {
    Model model = ModelFactory.createDefaultModel();
    model.read(acl);
    this.acl = acl;
    this.dataset = DatasetFactory.create();
    this.dataset.addNamedModel(acl, model);
    return this;
  }

  /**
   * Set the HTTP method used to access the resource.
   * 
   * @param method
   * @return the builder
   */
  public WebQueryBuilder method(String method) {
    this.method = method;
    return this;
  }

  /**
   * Indicate that the access request would only append to the resource.
   * Do not use this method, if the request will modify the resource other than
   * appending.
   * 
   * @return the builder
   */
  public WebQueryBuilder isOnlyAppending() {
    this.isOnlyAppending = true;
    return this;
  }

  /**
   * Set the webid of the accessing agent.
   * 
   * @param webid
   * @return the builder
   */
  public WebQueryBuilder webid(String webid) {
    this.webid = webid;
    return this;
  }

  /**
   * Build a {@link WebQuery} from the information currently in the builder.
   * 
   * @param type the {@link QueryType} of the query to build
   * @return the {@link WebQuery}
   */
  public WebQuery build(QueryType type) {
    switch (type) {
      case QUERY_FOR_PUBLIC:
        return new QueryForPublic(dataset, resource, acl, method, isOnlyAppending);
      case QUERY_FOR_AUTHENTICATED:
        return new QueryForAuthenticated(dataset, resource, acl, method, isOnlyAppending);
      case QUERY_FOR_AGENT:
        if (webid == null)
          throw new IllegalArgumentException("Cannot build agent query for webid `null`");
        return new QueryForAgent(dataset, resource, acl, method, isOnlyAppending, webid);
      case QUERY_FOR_AGENTGROUP:
        if (webid == null)
          throw new IllegalArgumentException("Cannot build agent query for webid `null`");
        return new QueryForAgentGroup(dataset, resource, acl, method, isOnlyAppending, webid);
      default:
        throw new IllegalArgumentException("Cannot build query for type `" + type + "`");
    }
  }

  /**
   * Retrieve the access mode triple pattern which would appear for the provided
   * method and the provided boolean flags.
   * 
   * @param isControlling   indicate if the method is executed on an acl itself ->
   *                        is acl:Control
   * @param httpMethod      indicate the HTTP method used to access the resource
   * @param isOnlyAppending indicate if the request is only appending to the
   *                        resource or modifying it otherwise
   * @return a string with the corresponding triple pattern
   */
  static String getAccessModeTriples(boolean isControlling, String httpMethod, boolean isOnlyAppending) {
    if (isControlling) {
      return "?authorization acl:mode acl:Control .";
    }
    String result = "";
    switch (httpMethod) {
      case "GET":
        result = "?authorization acl:mode acl:Read .";
        break;
      case "POST":
      case "PATCH":
        // if we receive a request, which is NOT "only appending to a resource",
        // i.e. also deletes something, then we need acl:Write
        result = "?authorization acl:mode acl:Write .";
        if (isOnlyAppending) {
          // but if it is "only appending" then an acl:Append would also suffice.
          result = "{ " + result + " } UNION { ?authorization acl:mode acl:Append . }";
        }
        break;
      case "PUT":
      case "DELETE":
        result = "?authorization acl:mode acl:Write .";
        break;
      default:
        throw new IllegalArgumentException("Unknown HTTP method");
    }
    return result;
  }

}
