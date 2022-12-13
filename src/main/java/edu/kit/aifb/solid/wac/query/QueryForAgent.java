package edu.kit.aifb.solid.wac.query;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;

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
        ?authorization acl:agent <http://example.org/webid> .
    }
}
 * </pre>
 */
class QueryForAgent implements WebQuery {

    private Dataset dataset;
    private String queryString;

    public QueryForAgent(Dataset dataset, String resource, String acl, String method, boolean isOnlyAppending,
            String webid) {
        this.dataset = dataset;
        boolean isControlling = resource.equals(acl);
        String targetResource = WebAccessControlBouncer.findCorrespondingResource(acl);
        String modeTriples = WebQueryBuilder.getAccessModeTriples(isControlling, method, isOnlyAppending);
        this.queryString = String.format("""
                PREFIX acl: <http://www.w3.org/ns/auth/acl#>
                ASK {
                GRAPH <%s> {
                ?authorization a acl:Authorization .
                ?authorization acl:accessTo <%s> .
                %s
                ?authorization acl:agent <%s> .
                }
                }
                """, acl, targetResource, modeTriples, webid);
    }

    @Override
    public boolean execOnWeb() {
        QueryExecution qexec = QueryExecutionFactory.create(this.queryString, this.dataset);
        return qexec.execAsk();
    }

}
