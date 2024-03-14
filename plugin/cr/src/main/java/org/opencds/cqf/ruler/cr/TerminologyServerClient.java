package org.opencds.cqf.ruler.cr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TerminologyServerClient {

	private final FhirContext ctx;

	private String username;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	private String apiKey;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
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
		// TODO why does it only work with this URL? when using authoritative source we get 400
		String BASE_URL = "https://cts.nlm.nih.gov/fhir/";
		IGenericClient fhirClient = ctx.newRestfulGenericClient(BASE_URL);
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
}
