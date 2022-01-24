package org.opencds.cqf.ruler.common.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;

public class ClientsTest {
	@Test
	public void testCreateClient() {
		IGenericClient client = Clients.forUrl(FhirContext.forR4Cached(), "http://test.com");

		assertNotNull(client);
		assertEquals("http://test.com", client.getServerBase());
	}

	@Test
	public void testRegisterAuth() {
		IGenericClient client = Clients.forUrl(FhirContext.forR4Cached(), "http://test.com");
		Clients.registerBasicAuth(client, "user", "password");

		List<Object> interceptors = client.getInterceptorService().getAllRegisteredInterceptors();

		Object authInterceptor = interceptors.get(0);
		assertTrue(authInterceptor instanceof BasicAuthInterceptor);
	}

	@Test
	public void testRegisterHeaders() {
		IGenericClient client = Clients.forUrl(FhirContext.forR4Cached(), "http://test.com");
		Clients.registerHeaders(client, "Basic: XYZ123");

		List<Object> interceptors = client.getInterceptorService().getAllRegisteredInterceptors();

		Object interceptor = interceptors.get(0);
		assertTrue(interceptor instanceof HeaderInjectionInterceptor);
	}

	@Test
	public void testRejectInvalidHeaders() {
		IGenericClient client = Clients.forUrl(FhirContext.forR4Cached(), "http://test.com");
		assertThrows(IllegalArgumentException.class, () -> {
			Clients.registerHeaders(client, "BasicXYZ123");
		});
	}

	@Test
	public void testClientForEndpoint() {
		Endpoint endpoint = new Endpoint();
		endpoint.setAddress("http://test.com");

		endpoint.setHeader(Collections.singletonList(new StringType("Basic: XYZ123")));
		IGenericClient client = Clients.forEndpoint(endpoint);

		assertEquals("http://test.com", client.getServerBase());
		List<Object> interceptors = client.getInterceptorService().getAllRegisteredInterceptors();

		Object interceptor = interceptors.get(0);
		assertTrue(interceptor instanceof HeaderInjectionInterceptor);
	}
}
