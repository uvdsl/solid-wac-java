package edu.kit.aifb.solid.wac.exception;

/**
 * To be raised when there is a SOLID-OIDC specific issue.
 */
public class SolidWacException extends Exception {

    private int statusCode;

    /**
     * StatusCode constructor.
     * 
     * @param statusCode The status code of the exception, e.g. 401, 403, ...
     * @param msg        The error message.
     */
    public SolidWacException(int statusCode, String msg) {
        super(msg);
        this.statusCode = statusCode;
    }

    /**
     * Get the status code of the exception - typically in the style of HTTP
     * response codes: 401 unauthenticated, 403 unauthorized, 500 server error
     * 
     * @return
     */
    public int getStatusCode() {
        return this.statusCode;
    }
}
