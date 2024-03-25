package org.opencds.cqf.ruler.cr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TerminologyServerClient {

	private final FhirContext ctx;

	private String username;

	private String getUsername() {
		return username;
	}

	private void setUsername(String username) {
		this.username = username;
	}

	private String apiKey;

	private String getApiKey() {
		return apiKey;
	}

	private void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	public TerminologyServerClient() {
		ctx = FhirContext.forR4();
	}

	public TerminologyServerClient(String username, String apiKey) {
		this();
		setUsername(username);
		setApiKey(apiKey);
	}

	public ValueSet expand(ValueSet valueSet, String authoritativeSource, Parameters expansionParameters) {
		IGenericClient fhirClient = ctx.newRestfulGenericClient(getAuthoritativeSourceBase(authoritativeSource));
		fhirClient.registerInterceptor(getAuthInterceptor(getUsername(), getApiKey()));

		// Invoke by Value Set ID
		return fhirClient
			.operation()
			.onInstance(valueSet.getId())
			.named("$expand")
			.withParameters(expansionParameters)
			.returnResourceType(ValueSet.class)
			.execute();
	}

	private AdditionalRequestHeadersInterceptor getAuthInterceptor(String username, String apiKey) {
		String authString = StringUtils.join("Basic ", Base64.getEncoder()
			 .encodeToString(StringUtils.join(username, ":", apiKey).getBytes(StandardCharsets.UTF_8)));
		 AdditionalRequestHeadersInterceptor authInterceptor = new AdditionalRequestHeadersInterceptor();
		 authInterceptor.addHeaderValue("Authorization", authString);
		 return authInterceptor;
	 }

 	// Strips resource and id from the authoritative source URL, these are not needed as the client constructs the URL.
	// Converts http URLs to https
	private String getAuthoritativeSourceBase(String authoritativeSource) {
		authoritativeSource = authoritativeSource.substring(0, authoritativeSource.indexOf(Canonicals.getResourceType(authoritativeSource)));
		if (authoritativeSource.startsWith("http://")) {
			authoritativeSource = authoritativeSource.replaceFirst("http://", "https://");
		}
		return authoritativeSource;
	}
}
