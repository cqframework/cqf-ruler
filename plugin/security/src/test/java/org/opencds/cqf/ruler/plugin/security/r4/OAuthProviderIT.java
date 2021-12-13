package org.opencds.cqf.ruler.plugin.security.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.security.SecurityConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
	SecurityConfig.class }, properties = {
	// Override is currently required when using MDM as the construction of the MDM
	// beans are ambiguous as they are constructed multiple places. This is evident
	// when running in a spring boot environment
	"spring.main.allow-bean-definition-overriding=true",
	"spring.batch.job.enabled=false",
	"spring.datasource.url=jdbc:h2:mem:dbr4-mt",
	"hapi.fhir.fhir_version=r4",
	"hapi.fhir.tester_enabled=false",
	"hapi.fhir.security.enabled=true"
})
public class OAuthProviderIT {
	private IGenericClient ourClient;
	private FhirContext ourCtx;

	@LocalServerPort
	private int port;

	@BeforeEach
	void beforeEach() {

		ourCtx = FhirContext.forR4Cached();
		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		String ourServerBase = "http://localhost:" + port + "/fhir/";
		ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
//		ourClient.registerInterceptor(new LoggingInterceptor(false));
	}

	@Test
	public void testOAuthConfig() {
		CapabilityStatement cs = ourClient.capabilities().ofType(CapabilityStatement.class).execute();

		assertNotNull(cs);
		assertEquals(true, cs.getRestFirstRep().getSecurity().getCors());
		assertEquals("http://hl7.org/fhir/restful-security-service", cs.getRestFirstRep().getSecurity().getService().stream().findAny().get().getCodingFirstRep().getSystem());

	}
}