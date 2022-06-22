package org.opencds.cqf.ruler.security.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.devtools.DevToolsConfig;
import org.opencds.cqf.ruler.security.SecurityConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {AuthenticatorInterceptorIT.class,
	SecurityConfig.class, CrConfig.class, CqlConfig.class, DevToolsConfig.class }, properties = {
	"hapi.fhir.fhir_version=r4", "hapi.fhir.security.basic_auth.enabled=true",
	"hapi.fhir.security.basic_auth.username=someuser",
	"hapi.fhir.security.basic_auth.password=thepassword"
})
public class AuthenticatorInterceptorIT extends RestIntegrationTest {

	@Test
	public void testMeasureEvaluateAuth() throws Exception {
		String bundleAsText = stringFromResource("test-bundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle)
			.withAdditionalHeader("Authorization", "Basic c29tZXVzZXI6dGhlcGFzc3dvcmQ=")
			.execute();

	}

	@Test
	public void testMeasureEvaluateAuthExceptionWithoutHeader() throws Exception {
		String bundleAsText = stringFromResource("test-bundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsText);

		assertThrows(Exception.class, () -> {
			getClient().transaction().withBundle(bundle).execute();
		});
	}

	@Test
	public void testMeasureEvaluateAuthExceptionWrongAuthInfo() throws Exception {
		String bundleAsText = stringFromResource("test-bundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsText);


		assertThrows(Exception.class, () -> {
			getClient().transaction().withBundle(bundle)
				.withAdditionalHeader("Authorization", "Basic blahblah")
				.execute();
		});
	}
}
