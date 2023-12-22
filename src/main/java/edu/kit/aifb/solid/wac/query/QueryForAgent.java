package edu.kit.aifb.solid.wac.query;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;

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
 * ?authorization acl:agent <http://example.org/webid> .
 * }
 * }
 * </pre>
 */
class QueryForAgent extends WacQuery {

    private String forAgentWebId;

    /**
     *
     * @param inAuthoritativeACL
     * @param onResource
     * @param isLookingForInheritedRule
     * @param forMode
     * @param forAgentWebId
     */
    public QueryForAgent(Dataset inAuthoritativeACL, String onResource, boolean isLookingForInheritedRule, String forMode, String forAgentWebId) {
        super(inAuthoritativeACL, onResource, isLookingForInheritedRule, forMode);
        if (forAgentWebId == null) {
            throw new IllegalArgumentException("Cannot build agent query for webid `null`");
        }
        this.forAgentWebId = forAgentWebId;
        String agentTriple = " " + this.VARIABLE_FOR_AUTHORIZATION + " acl:agent <" + forAgentWebId + "> .";
        this.appendToQueryBGPs(agentTriple);
    }

    @Override
    public String exec() {
        if (this.forAgentWebId == null) {
            throw new IllegalArgumentException("Cannot execute agent query for webid `null`");
        }
        String queryString = this.getQueryWithCurrentBGPs();
        QueryExecution qexec = QueryExecutionFactory.create(queryString, this.authoritativeACL);
        ResultSet results = qexec.execSelect();
        if (results.hasNext()) {
            return results.next().getResource(this.VARIABLE_FOR_AUTHORIZATION).getURI();
        }
        return null;
    }

}
