package edu.kit.aifb.solid.wac.query;

/**
 * types of access rules,
 * correspond to classes in this package!
 * Are used to build queries at the {@link WebQueryBuilder}.
 */
public enum QueryType {
    QUERY_FOR_PUBLIC,
    QUERY_FOR_AUTHENTICATED,
    QUERY_FOR_AGENT,
    QUERY_FOR_AGENTGROUP;
}
