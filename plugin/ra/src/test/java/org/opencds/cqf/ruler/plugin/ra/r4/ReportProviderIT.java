package org.opencds.cqf.ruler.plugin.ra.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.ra.RAConfig;
import org.opencds.cqf.ruler.plugin.ra.RAProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
        RAConfig.class }, properties ="hapi.fhir.fhir_version=r4")
public class ReportProviderIT {
    private IGenericClient ourClient;
    private FhirContext ourCtx;

    @LocalServerPort
    private int port;

    @BeforeEach
	void beforeEach() {

		ourCtx = FhirContext.forR4();
		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		String ourServerBase = "http://localhost:" + port + "/fhir/";
		ourClient = ourCtx.newRestfulGenericClient(ourServerBase);

        // myRaProperties.getReport().setEndpoint(ourServerBase);


//		ourClient.registerInterceptor(new LoggingInterceptor(false));
	}

    @Test
    public void testReport() {

        // TODO: Need to load a questionnaire url and set it for this
        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setId("1");

        Parameters params = new Parameters();
        params.addParameter().setName("questionnaireResponse").setResource(response);


        // TODO: Need to validate that the bundle is returned and also submitted
        Exception e = assertThrows(InternalErrorException.class, () -> {
            ourClient.operation().onType(QuestionnaireResponse.class).named("$report")
            .withParameters(params)
            .returnResourceType(Bundle.class)
            .execute();
        });
    }
}
