package org.opencds.cqf.ruler.cr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.util.ParametersUtil;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

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
//		ctx = FhirContext.forR4();
		setUsername(username);
		setApiKey(apiKey);
	}

	public ValueSet expand(ValueSet valueSet, String authoritativeSource, Parameters expansionParameters) {
		IGenericClient fhirClient = ctx.newRestfulGenericClient(authoritativeSource);
		fhirClient.registerInterceptor(getAuthInterceptor(getUsername(), getApiKey()));

		IBaseParameters parameters = ParametersUtil.newInstance(ctx);

//		ParametersUtil.addParameterToParameters(ctx, parameters, "valueSet", valueSet);

		// Invoke by ID rather than passing full VS in body
		return fhirClient
			.operation()
			.onInstance(valueSet.getId())
			.named("$expand")
			.withParameters(expansionParameters)
			.returnResourceType(ValueSet.class)
			.execute();
	}

//	public ValueSet expand(ValueSet valueSet, String authoritativeSource, String systemVersion, String username, String apiKey) {
//		IGenericClient fhirClient = ctx.newRestfulGenericClient(authoritativeSource);
//		fhirClient.registerInterceptor(getAuthInterceptor(username, apiKey));
//
//		IBaseParameters parameters = ParametersUtil.newInstance(ctx);
//		ParametersUtil.addParameterToParameters(ctx, parameters, "valueSet", valueSet);
//		ParametersUtil.addParameterToParameters(ctx, parameters, "system-version", systemVersion);
//		return fhirClient.operation().onType("ValueSet").named("$expand")
//			.withParameters(parameters).returnResourceType(ValueSet.class).execute();
//    }

	 private AdditionalRequestHeadersInterceptor getAuthInterceptor(String username, String apiKey) {
		String authString = StringUtils.join("Basic ", Base64.getEncoder()
			 .encodeToString(StringUtils.join(username, ":", apiKey).getBytes(StandardCharsets.UTF_8)));
		 AdditionalRequestHeadersInterceptor authInterceptor = new AdditionalRequestHeadersInterceptor();
		 authInterceptor.addHeaderValue("Authorization", authString);
		 return authInterceptor;
	 }
}
