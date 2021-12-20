package org.opencds.cqf.ruler.plugin.utility;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;

/**
 * This interface provides utility functions for creating IGenericClients and
 * setting up authentication
 */
public interface ClientUtilities {

	/**
	 * Creates an IGenericClient for the given url. Defaults to NEVER
	 * ServerValidationMode
	 * 
	 * @param theFhirVersionEnum the FHIR version to create a client for
	 * @param theUrl             the server base url to connect to
	 * @return IGenericClient for the given url
	 */
	public default IGenericClient createClient(FhirVersionEnum theFhirVersionEnum, String theUrl) {
		return createClient(FhirContext.forCached(theFhirVersionEnum), theUrl);
	}

	/**
	 * Creates an IGenericClient for the given url. Defaults to NEVER
	 * ServerValidationMode
	 * 
	 * @param theFhirContext the FhirContext to use to create the client
	 * @param theUrl         the server base url to connect to
	 * @return IGenericClient for the given url
	 */
	public default IGenericClient createClient(FhirContext theFhirContext, String theUrl) {
		return createClient(theFhirContext, theUrl, ServerValidationModeEnum.NEVER);
	}

	/**
	 * Creates an IGenericClient for the given url.
	 * 
	 * @param theFhirVersionEnum          the FHIR version to create a client for
	 * @param theUrl                      the server base url to connect to
	 * @param theServerValidationModeEnum the ServerValidationMode to use
	 * @return IGenericClient for the given url, with the server validation mode set
	 */
	public default IGenericClient createClient(FhirVersionEnum theFhirVersionEnum, String theUrl,
			ServerValidationModeEnum theServerValidationModeEnum) {
		return createClient(FhirContext.forCached(theFhirVersionEnum), theUrl, theServerValidationModeEnum);
	}

	/**
	 * Creates an IGenericClient for the given url.
	 * 
	 * @param theFhirContext              the FhirContext to use to create the
	 *                                    client
	 * @param theUrl                      the server base url to connect to
	 * @param theServerValidationModeEnum the ServerValidationMode to use
	 * @return IGenericClient for the given url, with the server validation mode set
	 */
	public default IGenericClient createClient(FhirContext theFhirContext, String theUrl,
			ServerValidationModeEnum theServerValidationModeEnum) {
		theFhirContext.getRestfulClientFactory().setServerValidationMode(theServerValidationModeEnum);
		return theFhirContext.newRestfulGenericClient(theUrl);
	}

	/**
	 * Creates an IGenericClient for the given Endpoint.
	 * 
	 * @param theEndpoint the Endpoint to connect to
	 * @return IGenericClient for the given Endpoint, with appropriate header
	 *         interceptors set up
	 */
	public default IGenericClient createClient(org.hl7.fhir.dstu3.model.Endpoint theEndpoint) {
		return createClient(FhirContext.forDstu3Cached(), theEndpoint);
	}

	/**
	 * Creates an IGenericClient for the given Endpoint.
	 * 
	 * @param theFhirContext the FhirContext to use to create the client
	 * @param theEndpoint    the Endpoint to connect to
	 * @return IGenericClient for the given Endpoint, with appropriate header
	 *         interceptors set up
	 */
	public default IGenericClient createClient(FhirContext theFhirContext,
			org.hl7.fhir.dstu3.model.Endpoint theEndpoint) {
		IGenericClient client = createClient(theFhirContext, theEndpoint.getAddress());
		if (theEndpoint.hasHeader()) {
			List<String> headerList = theEndpoint.getHeader().stream().map(headerString -> headerString.asStringValue())
					.collect(Collectors.toList());
			registerHeaders(client, headerList);
		}
		return client;
	}

	/**
	 * Creates an IGenericClient for the given Endpoint.
	 * 
	 * @param theEndpoint the Endpoint to connect to
	 * @return IGenericClient for the given Endpoint, with appropriate header
	 *         interceptors set up
	 */
	public default IGenericClient createClient(org.hl7.fhir.r4.model.Endpoint theEndpoint) {
		return createClient(FhirContext.forR4Cached(), theEndpoint);
	}

	/**
	 * Creates an IGenericClient for the given Endpoint.
	 * 
	 * @param theFhirContext the FhirContext to use to create the client
	 * @param theEndpoint    the Endpoint to connect to
	 * @return IGenericClient for the given Endpoint, with appropriate header
	 *         interceptors set up
	 */
	public default IGenericClient createClient(FhirContext theFhirContext, org.hl7.fhir.r4.model.Endpoint theEndpoint) {
		IGenericClient client = createClient(theFhirContext, theEndpoint.getAddress());
		if (theEndpoint.hasHeader()) {
			List<String> headerList = theEndpoint.getHeader().stream().map(headerString -> headerString.asStringValue())
					.collect(Collectors.toList());
			registerHeaders(client, headerList);
		}
		return client;
	}

	/**
	 * Creates an IGenericClient for the given Endpoint.
	 * 
	 * @param theEndpoint the Endpoint to connect to
	 * @return IGenericClient for the given Endpoint, with appropriate header
	 *         interceptors set up
	 */
	public default IGenericClient createClient(org.hl7.fhir.r5.model.Endpoint theEndpoint) {
		return createClient(FhirContext.forR4Cached(), theEndpoint);
	}

	/**
	 * Creates an IGenericClient for the given Endpoint.
	 * 
	 * @param theFhirContext the FhirContext to use to create the client
	 * @param theEndpoint    the Endpoint to connect to
	 * @return IGenericClient for the given Endpoint, with appropriate header
	 *         interceptors set up
	 */
	public default IGenericClient createClient(FhirContext theFhirContext, org.hl7.fhir.r5.model.Endpoint theEndpoint) {
		IGenericClient client = createClient(theFhirContext, theEndpoint.getAddress());
		if (theEndpoint.hasHeader()) {
			List<String> headerList = theEndpoint.getHeader().stream().map(headerString -> headerString.asStringValue())
					.collect(Collectors.toList());
			registerHeaders(client, headerList);
		}
		return client;
	}

	/**
	 * Registers HeaderInjectionInterceptors on a client.
	 * 
	 * @param theClient  the client to add headers to
	 * @param theHeaders an Array of Strings representing headers to add
	 */
	public default void registerHeaders(IGenericClient theClient, String... theHeaders) {
		this.registerHeaders(theClient, Arrays.asList(theHeaders));
	}

	/**
	 * Registers HeaderInjectionInterceptors on a client
	 * 
	 * @param theClient     the client to add headers to
	 * @param theHeaderList a List of Strings representing headers to add
	 */
	public default void registerHeaders(IGenericClient theClient, List<String> theHeaderList) {
		Map<String, String> headerMap = setupHeaderMap(theHeaderList);
		for (Map.Entry<String, String> entry : headerMap.entrySet()) {
			IClientInterceptor headInterceptor = new HeaderInjectionInterceptor(entry.getKey(), entry.getValue());
			theClient.registerInterceptor(headInterceptor);
		}
	}

	/**
	 * Registers BasicAuthInterceptors on a client. This is useful when you have a
	 * username and password.
	 * 
	 * @param theClient   the client to register basic auth on
	 * @param theUsername the username
	 * @param thePassword the password
	 */
	public default void registerBasicAuth(IGenericClient theClient, String theUsername, String thePassword) {
		if (theUsername != null) {
			BasicAuthInterceptor authInterceptor = new BasicAuthInterceptor(theUsername, thePassword);
			theClient.registerInterceptor(authInterceptor);
		}
	}

	/**
	 * Registers BearerAuthInterceptors on a client. This is useful when you have a
	 * bearer token.
	 * 
	 * @param theClient the client to register BearerToken authentication on
	 * @param theToken  the bearer token to register
	 */
	public default void registerBearerTokenAuth(IGenericClient theClient, String theToken) {
		if (theToken != null) {
			BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(theToken);
			theClient.registerInterceptor(authInterceptor);
		}
	}

	/**
	 * Parses a list of headers into their constituent parts. Used to prep the
	 * headers for registration with HeaderInjectionInterceptors
	 * 
	 * @param theHeaderList a List of Strings representing headers to create
	 * @return key-value pairs of headers
	 */
	public default Map<String, String> setupHeaderMap(List<String> theHeaderList) {
		Map<String, String> headerMap = new HashMap<String, String>();
		String leftAuth = null;
		String rightAuth = null;
		if (theHeaderList.size() < 1 || theHeaderList.isEmpty()) {
			leftAuth = null;
			rightAuth = null;
			headerMap.put(leftAuth, rightAuth);
		} else {
			for (String header : theHeaderList) {
				if (!header.contains(":")) {
					throw new IllegalArgumentException("Endpoint header must contain \":\" .");
				}
				String[] authSplit = header.split(":");
				leftAuth = authSplit[0];
				rightAuth = authSplit[1];
				headerMap.put(leftAuth, rightAuth);
			}

		}
		return headerMap;
	}
}
