package org.opencds.cqf.ruler.hello;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
        HelloWorldConfig.class }, properties = {
            // Override is currently required when using MDM as the construction of the MDM
            // beans are ambiguous as they are constructed multiple places. This is evident
            // when running in a spring boot environment
            "spring.main.allow-bean-definition-overriding=true",
            "spring.batch.job.enabled=false",
            "spring.datasource.url=jdbc:h2:mem:dbr4-mt",
            "hapi.fhir.fhir_version=r4",
            "hello.world.message=Howdy"

})
public class HelloWorldProviderIT {
    private IGenericClient ourClient;

	 @Autowired
	 FhirContext ourCtx;

    @LocalServerPort
    private int port;

    @BeforeEach
	void beforeEach() {
		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		String ourServerBase = "http://localhost:" + port + "/fhir/";
		ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
//		ourClient.registerInterceptor(new LoggingInterceptor(false));
	}

    @Test
    public void testHelloWorldConfig() {

        OperationOutcome outcome = ourClient.operation().onServer().named("$hello-world")
                .withNoParameters(Parameters.class).returnResourceType(OperationOutcome.class).execute();

        assertNotNull(outcome);
        assertEquals(outcome.getIssueFirstRep().getDiagnostics(), "Howdy");
    }
}
