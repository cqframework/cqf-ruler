package org.opencds.cqf.ruler.plugin.cdshooks.r4;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.cdshooks.CdsHooksConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = { Application.class,
	CdsHooksConfig.class }, properties = {
	// Override is currently required when using MDM as the construction of the MDM
	// beans are ambiguous as they are constructed multiple places. This is evident
	// when running in a spring boot environment
	"spring.main.allow-bean-definition-overriding=true",
	"spring.batch.job.enabled=false",
	"spring.datasource.url=jdbc:h2:mem:dbr4-mt",
	"hapi.fhir.fhir_version=r4",
	"hapi.fhir.tester_enabled=false",
	"hapi.fhir.cdshooks.enabled=true"
})
public class CdsHooksServletIT {
	private IGenericClient ourClient;
	private FhirContext ourCtx;

	@Autowired
	AppProperties myAppProperties;

	@LocalServerPort
	private int port;

	String ourCdsBase;

	@BeforeEach
	void beforeEach() {

		ourCtx = FhirContext.forR4Cached();
		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);

		String ourServerBase = "http://localhost:" + port + "/fhir/";
		ourCdsBase = "http://localhost:" + port + "/cds-services";
		myAppProperties.setServer_address(ourServerBase);
		myAppProperties.setCors(new AppProperties.Cors());
		ourClient = ourCtx.newRestfulGenericClient(ourCdsBase);
//		ourClient.registerInterceptor(new LoggingInterceptor(false));
	}


	@Test
	public void testGetCdsServices()  {
		assertEquals(DataFormatException.class, assertThrows(FhirClientConnectionException.class, () -> ourClient.search().byUrl(ourClient.getServerBase()).execute()).getCause().getClass());
	}

}
