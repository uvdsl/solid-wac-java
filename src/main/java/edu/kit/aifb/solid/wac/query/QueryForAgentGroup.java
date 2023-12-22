package edu.kit.aifb.solid.wac.query;

import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import edu.kit.aifb.solid.wac.Namespaces;

/**
 * Example:
 *
 * <pre>
 * PREFIX acl: <http://www.w3.org/ns/auth/acl#>
 * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
 * PREFIX vcard: <http://www.w3.org/2006/vcard/ns#>
 * ASK {
 * GRAPH <http://localhost:8080/marmotta/ldp/test.acl> {
 * ?authorization a acl:Authorization .
 * ?authorization acl:accessTo <http://localhost:8080/marmotta/ldp/test> .
 * { ?authorization acl:mode acl:Write . }
 * UNION
 * { ?authorization acl:mode acl:Append . }
 * ?authorization acl:agentGroup ?group .
 * BIND(
 * REPLACE(?group, "(#|#.*)", "" )
 * AS ?grp )
 * }
 * GRAPH ?grp {
 * ?group vcard:hasMember <http://example.org/webid>.
 * }
 * }
 * </pre>
 */
class QueryForAgentGroup extends WacQuery {

    private String forAgentWebId;
    private Map<String, Dataset> agentGroupsMap;
    private final String VARIABLE_FOR_GROUP = "?group";

    /**
     *
     * @param inAuthoritativeACL
     * @param onResource
     * @param isLookingForInheritedRule
     * @param forMode
     * @param forAgentWebId
     * @param agentGroupsMap
     */
    public QueryForAgentGroup(Dataset inAuthoritativeACL, String onResource, boolean isLookingForInheritedRule, String forMode, String forAgentWebId, Map<String, Dataset> agentGroupsMap) {
        super(inAuthoritativeACL, onResource, isLookingForInheritedRule, forMode);
        if (forAgentWebId == null) {
            throw new IllegalArgumentException("Cannot build agent query for webid `null`");
        }
        this.forAgentWebId = forAgentWebId;
        this.agentGroupsMap = agentGroupsMap;
        String agentTriple = " " + this.VARIABLE_FOR_AUTHORIZATION + " acl:agentGroup " + this.VARIABLE_FOR_GROUP + " .";
        this.appendToQueryBGPs(agentTriple);
    }

    /**
     * override to also return group
     */
    @Override
    protected String getQueryWithCurrentBGPs() {
        return String.format("""
                                PREFIX acl: <%s>
                                SELECT %s %s {
                                GRAPH ?graph {
                                    %s
                                }
                                }
                            """, Namespaces.ACL, this.VARIABLE_FOR_AUTHORIZATION, this.VARIABLE_FOR_GROUP, this.getQueryBGPs());
    }

    /**
     * Dynamically generate a new query to look up the agent group.
     *
     * @param groupGraphName
     * @return true or false
     */
    private String generateAgentQueryString(String groupGraphName, String groupName) {
        return String.format("""
                PREFIX vcard: <http://www.w3.org/2006/vcard/ns#>
                ASK {
                GRAPH <%s> {
                <%s> vcard:hasMember <%s>.
                }
                }
                """, groupGraphName, groupName, this.forAgentWebId);
    }

    /**
     * Find any applicable agent group and derefence their URIs to check if the
     * agent is a member of any.
     *
     * @return the {@code String} of the retrieved access control rule URI or
     * {@code null} if no rule was found
     */
    @Override
    public String exec() {
        if (this.forAgentWebId == null) {
            throw new IllegalArgumentException("Cannot execute agent query for webid `null`");
        }
        String queryForGroupString = this.getQueryWithCurrentBGPs();
        QueryExecution qexec = QueryExecutionFactory.create(queryForGroupString, this.authoritativeACL);
        ResultSet results = qexec.execSelect();
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            String groupName = soln.getResource(this.VARIABLE_FOR_GROUP).getURI();
            Dataset datasetGroup = this.agentGroupsMap.get(groupName.split("#")[0]);
            String queryStringForAgent = generateAgentQueryString(groupName.split("#")[0], groupName);
            QueryExecution qexecAgent = QueryExecutionFactory.create(queryStringForAgent, datasetGroup);
            boolean resultAgent = qexecAgent.execAsk();
            if (resultAgent) {
                return soln.getResource(this.VARIABLE_FOR_AUTHORIZATION).getURI();
            }
        }
        return null;
    }

}
