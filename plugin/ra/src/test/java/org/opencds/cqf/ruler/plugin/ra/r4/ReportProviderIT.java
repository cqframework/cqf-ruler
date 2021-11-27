package org.opencds.cqf.ruler.plugin.ra.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.ra.RAConfig;
import org.opencds.cqf.ruler.plugin.ra.RAProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
        RAConfig.class }, properties = {"hapi.fhir.fhir_version=r4", "hapi.fhir.ra.enabled=true" })
public class ReportProviderIT  {
    private Logger ourLog = LoggerFactory.getLogger(ReportProviderIT.class);

    private IGenericClient ourClient;
    private FhirContext ourCtx;
    
    @Autowired
    private DaoRegistry ourRegistry;

    @Autowired
    private RAProperties myRaProperties;

    @LocalServerPort
    private int port;

    @BeforeEach
	void beforeEach() {

		ourCtx = FhirContext.forCached(FhirVersionEnum.R4);
		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		String ourServerBase = "http://localhost:" + port + "/fhir/";
		ourClient = ourCtx.newRestfulGenericClient(ourServerBase);	}

    @Test
    public void testReport() throws IOException {

        //loadResource("mypain-questionnaire.json", ourCtx, ourRegistry);


        //QuestionnaireResponse test = (QuestionnaireResponse)ourCtx.newJsonParser().parseResource(stringFromResource("mypain-questionnaire-response.json"));

        Parameters params = new Parameters();
        params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        params.addParameter().setName("subject").setValue(new StringType("testReport01"));
    
        Parameters actual = ourClient.operation().onType(MeasureReport.class).named("$report")
            .withParameters(params)
            .returnResourceType(Parameters.class)
            .execute();

            assertNull(actual);

        // Expecting one observation per item
        //assertEquals(5, actual.getEntry().size());

        // Ensure the Observations were saved to the local server
        //Observation obs = ourClient.read().resource(Observation.class).withId("IdFromBundle").execute();

        //assertNotNull(obs);

        // Check other observation properties
    }

    // @Test
    // public void testExtract_noQuestionnaireReference_throwsException() throws IOException {
    //     QuestionnaireResponse test = (QuestionnaireResponse)ourCtx.newJsonParser().parseResource(stringFromResource("mypain-questionnaire-response-no-url.json"));

    //     Parameters params = new Parameters();
    //     params.addParameter().setName("questionnaireResponse").setResource(test);

    //     assertThrows(InternalErrorException.class, () -> {
    //         ourClient.operation().onType(QuestionnaireResponse.class).named("$extract")
    //         .withParameters(params)
    //         .returnResourceType(Bundle.class)
    //         .execute();
    //     });
    // }
}
