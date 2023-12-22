package edu.kit.aifb.solid.wac.query;

import org.apache.jena.query.Dataset;

import edu.kit.aifb.solid.wac.Namespaces;

/**
 * Just a little abstract class to provide the base functionality for
 * WacQueries. Concrete implementations of this class specify how it is
 * expressed which agent may have access, e.g. using
 * {@code appendToQueryBGPs(String bgps)}.
 *
 * See also {@link WacQueryType}
 *
 */
public abstract class WacQuery {

    protected final String VARIABLE_FOR_AUTHORIZATION = "?authz";
    protected final Dataset authoritativeACL;
    private String queryBGPs;

    /**
     *
     * @param inAuthoritativeACL
     * @param onResource
     * @param isLookingForInheritedRule
     * @param forMode
     */
    public WacQuery(Dataset inAuthoritativeACL, String onResource, boolean isLookingForInheritedRule, String forMode) {
        this.authoritativeACL = inAuthoritativeACL;

        StringBuilder queryBGPsb = new StringBuilder();

        // basic
        String typeTriple = " " + this.VARIABLE_FOR_AUTHORIZATION + " a acl:Authorization .";
        queryBGPsb.append(typeTriple);
        queryBGPsb.append("\n");

        // accessTo or default
        String lookAtRuleFor = (isLookingForInheritedRule) ? "acl:default" : "acl:accessTo";
        String aclTargetTriples = " " + this.VARIABLE_FOR_AUTHORIZATION + " " + lookAtRuleFor + " <" + onResource + "> .";
        queryBGPsb.append(aclTargetTriples);
        queryBGPsb.append("\n");

        // accessMode
        String accessModeTriples = " " + this.VARIABLE_FOR_AUTHORIZATION + " acl:mode " + "<" + forMode + "> .";
        if (forMode.contains("Append")) {
            // if acl:Append is ok, then acl:Write is also acceptable
            accessModeTriples = "{ " + accessModeTriples + " } UNION { " + this.VARIABLE_FOR_AUTHORIZATION + " acl:mode" + "<" + Namespaces.ACL + "Write" + "> . }";
        }
        queryBGPsb.append(accessModeTriples);
        queryBGPsb.append("\n");

        queryBGPs = queryBGPsb.toString();

        // ! who? (public/authn/webid/group/...)
        // implementations of this class should 
        // (1) append the triples for the agent (public/authn/webid/group), e.g. using appendToQueryBGPs(...)
        // (2) specify how these BGPs are used in a query or multiple queries in exec().
    }

    protected void appendToQueryBGPs(String bgps) {
        this.queryBGPs += bgps;
    }

    protected String getQueryBGPs() {
        return this.queryBGPs;
    }

    /**
     * You may want to override this method, e.g. if you want to do link
     * traversal e.g. for agentGroup.
     *
     * @return a basic query string
     */
    protected String getQueryWithCurrentBGPs() {
        return String.format("""
                                PREFIX acl: <%s>
                                SELECT %s {
                                GRAPH ?graph {
                                    %s
                                }
                                }
                            """, Namespaces.ACL, this.VARIABLE_FOR_AUTHORIZATION, this.queryBGPs);
    }

    /**
     * execute the query
     *
     * @return the {@code String} of the retrieved access control rule URI or
     * {@code null} if no rule was found
     */
    public abstract String exec();
}
