package edu.kit.aifb.solid.wac.query;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class WebQueryBuilderTest {

    @Test
    public void testExceptionsMissingWebidAgent() {
        boolean ok = false;
        boolean err = false;
        try {
            WebQueryBuilder.newBuilder()
                    .webid(null)
                    .build(QueryType.QUERY_FOR_AGENT);
            ok = true;

        } catch (IllegalArgumentException e) {
            err = true;
        }
        assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
    }

    @Test
    public void testExceptionsMissingWebidAgentGroup() {
        boolean ok = false;
        boolean err = false;
        try {
            WebQueryBuilder.newBuilder()
                    .webid(null)
                    .build(QueryType.QUERY_FOR_AGENTGROUP);
            ok = true;

        } catch (IllegalArgumentException e) {
            err = true;
        }
        assertTrue("Expected: !ok && err; Result: ok=" + ok + " && err=" + err, !ok && err);
    }
}
