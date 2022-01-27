package org.opencds.cqf.ruler.test;

/**
 * This interface provides test utility functions for creating IGenericClients
 * and
 * setting up authentication
 */
public class Urls {
	private Urls() {}

	public static final String SERVER_URL = "http://localhost:%d/fhir";

	/**
	 * Creates an client url given a url template and port
	 * 
	 * @param theUrlTemplate the url template to use
	 * @param thePort        the port to use
	 * @return String for the client url
	 */
	public static String getUrl(String theUrlTemplate, Integer thePort) {
		return String.format(theUrlTemplate, thePort);
	}

	/**
	 * Creates an client url using the default url and port
	 * 
	 * @param thePort the port to use
	 * @return String for the client url
	 */
	public static String getUrl(Integer thePort) {
		return getUrl(SERVER_URL, thePort);
	}
}
