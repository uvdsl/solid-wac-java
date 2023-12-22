package edu.kit.aifb.solid.wac;

/**
 * Defines the bi-directional mapping between a {@code resource} and their
 * corresponding {@code .acl} ( i.e. in RDF via {@code acl:accessTo})
 *
 * @author ik1533
 */
public interface WacMapping {

    /**
     * Retrieve the URI string of the {@code .acl} of the input {@code resource}
     * URI string ({@code acl:accessTo})
     *
     * @param resource URI string
     * @return the URI string of the {@code .acl} of the input {@code resource}
     * URI string ({@code acl:accessTo})
     */
    public String getAcl(String resource);

    /**
     * Retrieve the URI string of the {@code resource} ({@code acl:accessTo}) of
     * the input {@code .acl} URI string
     *
     * @param acl URI string
     * @return the URI string of the {@code resource} ({@code acl:accessTo}) of
     * the input {@code .acl} URI string
     */
    public String getResource(String acl);
}
