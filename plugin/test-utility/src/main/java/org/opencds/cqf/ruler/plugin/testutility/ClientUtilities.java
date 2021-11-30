package org.opencds.cqf.ruler.plugin.testutility;

/**
 * This interface provides test utility functions for creating IGenericClients and
 * setting up authentication
 */
public interface ClientUtilities {
    public static String SERVER_URL = "http://localhost:%d/fhir";

        /**
     * Creates an client url given a url template and port
     * 
     * @param theUrlTemplate    the url template to use
     * @param thePort           the port to use
     * @return String for the client url
     */
    public default String getClientUrl(String theUrlTemplate, Integer thePort) {
        return String.format(theUrlTemplate, thePort);
    }
    
    /**
     * Creates an client url using the default url and port
     * 
     * @param thePort            the port to use
     * @return String for the client url
     */
    public default String getClientUrl(Integer thePort) {
        return getClientUrl(SERVER_URL, thePort);
    }
}
