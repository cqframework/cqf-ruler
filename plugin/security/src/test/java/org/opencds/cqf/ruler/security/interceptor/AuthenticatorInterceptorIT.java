package org.opencds.cqf.ruler.security.interceptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.HttpURLConnection;
import java.net.URL;

import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.devtools.DevToolsConfig;
import org.opencds.cqf.ruler.security.SecurityConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
		AuthenticatorInterceptorIT.class,
		SecurityConfig.class, DevToolsConfig.class }, properties = {
				"hapi.fhir.fhir_version=r4", "hapi.fhir.security.basic_auth.enabled=true",
				"hapi.fhir.security.basic_auth.username=someuser",
				"hapi.fhir.security.basic_auth.password=thepassword"
		})
public class AuthenticatorInterceptorIT extends RestIntegrationTest {

	@Test
	public void testPostBundleAuth() throws Exception {
		String bundleAsText = stringFromResource("test-bundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle)
				.withAdditionalHeader("Authorization", "Basic c29tZXVzZXI6dGhlcGFzc3dvcmQ=")
				.execute();

	}

	@Test
	public void testPostBundleAuthExceptionWithoutHeader() throws Exception {
		String bundleAsText = stringFromResource("test-bundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsText);

		Exception ex = assertThrows(AuthenticationException.class, () -> {
			getClient().transaction().withBundle(bundle).execute();
		});

		assertEquals("HTTP 401 : Missing or invalid Authorization header", ex.getMessage());
	}

	@Test
	public void testPostBundleAuthExceptionWrongAuthInfo() throws Exception {
		String bundleAsText = stringFromResource("test-bundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsText);

		Exception ex = assertThrows(AuthenticationException.class, () -> {
			getClient().transaction().withBundle(bundle)
					.withAdditionalHeader("Authorization", "Basic blahblah")
					.execute();
		});

		assertEquals("HTTP 401 : Missing or invalid Authorization header", ex.getMessage());
	}

	@Test
	public void testDeploymentProbeException() throws Exception {
		String serverBase = getClient().getServerBase();
		URL url = new URL(String.format("%smetadata", serverBase));
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		assertDoesNotThrow(() -> {
			con.getResponseCode();
		});
	}
}
