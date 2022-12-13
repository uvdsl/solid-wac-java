package edu.kit.aifb.solid.wac.query;

/**
 * just a little Interface to indicate that the Query will be executed on the
 * Web, if necessary.
 */
public interface WebQuery {
    /**
     * execute the ASK query,
     * if necessary look up graphs on the Web
     * 
     * @return true or false :)
     */
    public boolean execOnWeb();
}
