package org.opencds.cqf.ruler.cr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.util.ParametersUtil;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.ValueSet;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TerminologyServerClient {

	private final FhirContext ctx;

	public TerminologyServerClient() {
		ctx = FhirContext.forR4();
	}

	public ValueSet expand(ValueSet valueSet, String authoritativeSource, String systemVersion, String username, String apiKey) {
		IGenericClient fhirClient = ctx.newRestfulGenericClient(authoritativeSource);
		fhirClient.registerInterceptor(getAuthInterceptor(username, apiKey));

		IBaseParameters parameters = ParametersUtil.newInstance(ctx);
		ParametersUtil.addParameterToParameters(ctx, parameters, "valueSet", valueSet);
		ParametersUtil.addParameterToParameters(ctx, parameters, "system-version", systemVersion);
		return fhirClient.operation().onType("ValueSet").named("$expand")
			.withParameters(parameters).returnResourceType(ValueSet.class).execute();
    }

	 private AdditionalRequestHeadersInterceptor getAuthInterceptor(String username, String apiKey) {
		String authString = StringUtils.join("Basic ", Base64.getEncoder()
			 .encodeToString(StringUtils.join(username, ":", apiKey).getBytes(StandardCharsets.UTF_8)));
		 AdditionalRequestHeadersInterceptor authInterceptor = new AdditionalRequestHeadersInterceptor();
		 authInterceptor.addHeaderValue("Authorization", authString);
		 return authInterceptor;
	 }
}
