package edu.kit.aifb.solid.wac.query;

import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import edu.kit.aifb.solid.wac.WebAccessControlBouncer;

/**
 * Example:
 * 
 * <pre>
PREFIX acl: <http://www.w3.org/ns/auth/acl#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX vcard: <http://www.w3.org/2006/vcard/ns#>
ASK {
    GRAPH <http://localhost:8080/marmotta/ldp/test.acl> {
        ?authorization a acl:Authorization .
        ?authorization acl:accessTo <http://localhost:8080/marmotta/ldp/test> .
        { ?authorization acl:mode acl:Write . } 
        UNION 
        { ?authorization acl:mode acl:Append . }
        ?authorization acl:agentGroup ?group .
        BIND(
            REPLACE(?group, "(#|#.*)", "" )
            AS ?grp )
    }
    GRAPH ?grp {
        ?group vcard:hasMember <http://example.org/webid>.
    }
}
 * </pre>
 */
class QueryForAgentGroup implements WebQuery {

    private final String variableForGroup = "?group";
    private Dataset dataset;
    private String queryStringForGroup;
    private String webid;

    public QueryForAgentGroup(Dataset dataset, String resource, String acl, String method, boolean isOnlyAppending,
            String webid) {
        this.dataset = dataset;
        this.webid = webid;
        boolean isControlling = resource.equals(acl);
        String targetResource = WebAccessControlBouncer.findCorrespondingResource(acl);
        String modeTriples = WebQueryBuilder.getAccessModeTriples(isControlling, method, isOnlyAppending);
        this.queryStringForGroup = String.format("""
                PREFIX acl: <http://www.w3.org/ns/auth/acl#>
                SELECT %s {
                GRAPH <%s> {
                ?authorization a acl:Authorization .
                ?authorization acl:accessTo <%s> .
                %s
                ?authorization acl:agentGroup %s .
                }
                }
                """, variableForGroup, acl, targetResource, modeTriples, variableForGroup);
    }

    /**
     * Dynamically generate a new query to look up the agent group.
     * 
     * @param groupGraphName
     * @return true or false :)
     */
    private String generateAgentQueryString(String groupGraphName) {
        return String.format("""
                PREFIX vcard: <http://www.w3.org/2006/vcard/ns#>
                ASK {
                GRAPH <%s> {
                <%s> vcard:hasMember <%s>.
                }
                }
                """, groupGraphName, groupGraphName, this.webid);
    }

    /**
     * Find any applicable agent group and derefence their URIs to check if the
     * agent is a member of any.
     */
    @Override
    public boolean execOnWeb() {
        QueryExecution qexec = QueryExecutionFactory.create(this.queryStringForGroup, this.dataset);
        ResultSet results = qexec.execSelect();
        for (; results.hasNext();) {
            QuerySolution soln = results.next();
            Resource group = soln.getResource(variableForGroup);
            String groupGraphName = group.toString();
            Model groupModel = ModelFactory.createDefaultModel();
            try {
                groupModel.read(groupGraphName);
            } catch (HttpException e) {
                // do some logging?
                continue;
            }
            this.dataset.addNamedModel(groupGraphName, groupModel);
            String queryStringForAgent = generateAgentQueryString(groupGraphName);
            QueryExecution qexecAgent = QueryExecutionFactory.create(queryStringForAgent, this.dataset);
            if (qexecAgent.execAsk()) {
                return true;
            }
        }
        return false;
    }

}
