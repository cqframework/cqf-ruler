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

	private static final String VSAC_BASE_URL = "https://cts.nlm.nih.gov/fhir/";

	private static final String VSAC_USERNAME = System.getenv("VSAC_USERNAME");

	private static final String VSAC_API_KEY = System.getenv("VSAC_API_KEY");

	private static final String VSAC_CREDENTIALS = StringUtils.join(VSAC_USERNAME, ":", VSAC_API_KEY);

	private static final String VSAC_AUTH = StringUtils.join("Basic ", Base64.getEncoder()
		.encodeToString(VSAC_CREDENTIALS.getBytes(StandardCharsets.UTF_8)));

	private final FhirContext ctx;

	private IGenericClient fhirClient;

	public TerminologyServerClient() {
		ctx = FhirContext.forR4();
		fhirClient = ctx.newRestfulGenericClient(VSAC_BASE_URL);
		fhirClient.registerInterceptor(getAuthInterceptor());
	}

	public ValueSet expand(ValueSet valueSet, String authoritativeSource) {
		if (!authoritativeSource.equals(VSAC_BASE_URL)) {
			authoritativeSource = convertToHttps(authoritativeSource);
			if (authoritativeSource.startsWith(VSAC_BASE_URL)) {
				authoritativeSource = VSAC_BASE_URL;
			}
			fhirClient = ctx.newRestfulGenericClient(authoritativeSource);
			fhirClient.registerInterceptor(getAuthInterceptor());
		}
		IBaseParameters parameters = ParametersUtil.newInstance(ctx);
		ParametersUtil.addParameterToParameters(ctx, parameters, "valueSet", valueSet);
		return fhirClient.operation().onType("ValueSet").named("$expand")
			.withParameters(parameters).returnResourceType(ValueSet.class).execute();
    }

	 private String convertToHttps(String url) {
		if (url.startsWith("http://")) {
			return url.replace("http://", "https://");
		}
		return url;
	 }

	 private AdditionalRequestHeadersInterceptor getAuthInterceptor() {
		 AdditionalRequestHeadersInterceptor authInterceptor = new AdditionalRequestHeadersInterceptor();
		 authInterceptor.addHeaderValue("Authorization", VSAC_AUTH);
		 return authInterceptor;
	 }
}
