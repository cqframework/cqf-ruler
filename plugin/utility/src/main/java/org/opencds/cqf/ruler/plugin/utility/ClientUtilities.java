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
     * @param theFhirVersionEnum
     * @param theUrl
     * @return IGenericClient for the given url
     */
    public default IGenericClient createClient(FhirVersionEnum theFhirVersionEnum, String theUrl) {
        return createClient(FhirContext.forCached(theFhirVersionEnum), theUrl);
    }

    /**
     * @param theFhirContext
     * @param theUrl
     * @return IGenericClient for the given url
     */
    public default IGenericClient createClient(FhirContext theFhirContext, String theUrl) {
        return createClient(theFhirContext, theUrl, ServerValidationModeEnum.NEVER);
    }

    /**
     * @param theFhirVersionEnum
     * @param theUrl
     * @param theServerValidationModeEnum
     * @return IGenericClient for the given url, with the server validation mode set
     */
    public default IGenericClient createClient(FhirVersionEnum theFhirVersionEnum, String theUrl,
            ServerValidationModeEnum theServerValidationModeEnum) {
        return createClient(FhirContext.forCached(theFhirVersionEnum), theUrl, theServerValidationModeEnum);
    }

    /**
     * 
     * @param theFhirContext
     * @param theUrl
     * @param theServerValidationModeEnum
     * @return IGenericClient for the given url, with the server validation mode set
     */
    public default IGenericClient createClient(FhirContext theFhirContext, String theUrl,
            ServerValidationModeEnum theServerValidationModeEnum) {
        theFhirContext.getRestfulClientFactory().setServerValidationMode(theServerValidationModeEnum);
        return theFhirContext.newRestfulGenericClient(theUrl);
    }

    /**
     * @param theEndpoint
     * @return IGenericClient for the given Endpoint, with appropriate header
     *         interceptors set up
     */
    public default IGenericClient createClient(org.hl7.fhir.dstu3.model.Endpoint theEndpoint) {
        return createClient(FhirContext.forDstu3Cached(), theEndpoint);
    }

    /**
     * @param theFhirContext
     * @param theEndpoint
     * @return IGenericClient for the given Endpoint, with appropriate header
     *         interceptors set up
     */
    public default IGenericClient createClient(FhirContext theFhirContext, org.hl7.fhir.dstu3.model.Endpoint theEndpoint) {
        IGenericClient client = createClient(theFhirContext, theEndpoint.getAddress());
        if (theEndpoint.hasHeader()) {
            List<String> headerList = theEndpoint.getHeader().stream().map(headerString -> headerString.asStringValue())
                    .collect(Collectors.toList());
            registerHeaders(client, headerList);
        }
        return client;
    }

    /**
     * @param theEndpoint
     * @return IGenericClient for the given Endpoint, with appropriate header
     *         interceptors set up
     */
    public default IGenericClient createClient(org.hl7.fhir.r4.model.Endpoint theEndpoint) {
        return createClient(FhirContext.forR4Cached(), theEndpoint);
    }

    /**
     * @param theFhirContext
     * @param theEndpoint
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
     * @param theEndpoint
     * @return IGenericClient for the given Endpoint, with appropriate header
     *         interceptors set up
     */
    public default IGenericClient createClient(org.hl7.fhir.r5.model.Endpoint theEndpoint) {
        return createClient(FhirContext.forR4Cached(), theEndpoint);
    }

    /**
     * @param theFhirContext
     * @param theEndpoint
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
     * @param theClient
     * @param theHeaders
     */
    public default void registerHeaders(IGenericClient theClient, String... theHeaders) {
        this.registerHeaders(theClient, Arrays.asList(theHeaders));
    }

    /**
     * Registers HeaderInjectionInterceptors on a client
     * 
     * @param theClient
     * @param theHeaderList
     */
    public default void registerHeaders(IGenericClient theClient, List<String> theHeaderList) {
        Map<String, String> headerMap = setupHeaderMap(theHeaderList);
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            IClientInterceptor headInterceptor = new HeaderInjectionInterceptor(entry.getKey(), entry.getValue());
            theClient.registerInterceptor(headInterceptor);
        }
    }

    /**
     * Registers BasicAuthInterceptors on a client. This is useful when you have a username and password.
     * 
     * @param theClient
     * @param theUserId
     * @param thePassword
     */
    public default void registerBasicAuth(IGenericClient theClient, String theUserId, String thePassword) {
        if (theUserId != null) {
            BasicAuthInterceptor authInterceptor = new BasicAuthInterceptor(theUserId, thePassword);
            theClient.registerInterceptor(authInterceptor);
        }
    }

    /**
     * Registers BearerAuthInterceptors on a client. This is useful when you have a bearer token.
     * 
     * @param theClient
     * @param theToken
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
     * @param theHeaderList
     * @return ket-value pairs of headers
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
