package org.opencds.cqf.ruler.plugin.sdc.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.sdc.SDCConfig;
import org.opencds.cqf.ruler.plugin.sdc.SDCProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
        SDCConfig.class }, properties ="hapi.fhir.fhir_version=r4")
public class ExtractProviderIT implements IServerSupport {
    private IGenericClient ourClient;
    private FhirContext ourCtx;
    
    @Autowired
    private DaoRegistry ourRegistry;

    @LocalServerPort
    private int port;

    @BeforeEach
	void beforeEach() {

		ourCtx = FhirContext.forR4();
		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		String ourServerBase = "http://localhost:" + port + "/fhir/";
		ourClient = ourCtx.newRestfulGenericClient(ourServerBase);


        System.setProperty("hapi.fhir.sdc.extract.url", ourServerBase);

        // mySdcProperties.getExtract().setEndpoint(ourServerBase);


//		ourClient.registerInterceptor(new LoggingInterceptor(false));
	}

    @Test
    public void testExtract() throws IOException {

        loadResource("mypain-questionnaire.json", ourCtx, ourRegistry);


        QuestionnaireResponse test = (QuestionnaireResponse)ourCtx.newJsonParser().parseResource(stringFromResource("mypain-questionnaire-response.json"));

        Parameters params = new Parameters();
        params.addParameter().setName("questionnaireResponse").setResource(test);

        Bundle actual = ourClient.operation().onType(QuestionnaireResponse.class).named("$extract")
            .withParameters(params)
            .returnResourceType(Bundle.class)
            .execute();

        assertNotNull(actual);
        // TODO: Bundle Validation
    }

    @Test
    public void testExtract_noQuestionnaireReference_throwsException() throws IOException {
        QuestionnaireResponse test = (QuestionnaireResponse)ourCtx.newJsonParser().parseResource(stringFromResource("mypain-questionnaire-response-no-url.json"));

        Parameters params = new Parameters();
        params.addParameter().setName("questionnaireResponse").setResource(test);

        assertThrows(InternalErrorException.class, () -> {
            ourClient.operation().onType(QuestionnaireResponse.class).named("$extract")
            .withParameters(params)
            .returnResourceType(Bundle.class)
            .execute();
        });
    }
}
