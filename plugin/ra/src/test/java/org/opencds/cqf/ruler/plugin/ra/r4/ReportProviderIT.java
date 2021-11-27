package org.opencds.cqf.ruler.plugin.ra.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
import org.opencds.cqf.ruler.plugin.utility.ClientUtilities;
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
public class ReportProviderIT implements ClientUtilities {
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
		String ourServerBase = getClientUrl(myRaProperties.getReport().getEndpoint(), port);
	    ourClient = createClient(ourCtx, ourServerBase);
    }

    @Test
    public void testReport() throws IOException {

        //loadResource("mypain-questionnaire.json", ourCtx, ourRegistry);


        //QuestionnaireResponse test = (QuestionnaireResponse)ourCtx.newJsonParser().parseResource(stringFromResource("mypain-questionnaire-response.json"));

        Parameters params = new Parameters();
        params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        params.addParameter().setName("subject").setValue(new StringType("Patient/testReport01"));
    
        Parameters actual = ourClient.operation().onType(MeasureReport.class).named("$report")
            .withParameters(params)
            .returnResourceType(Parameters.class)
            .execute();

            assertNotNull(actual);

        // Expecting one observation per item
        //assertEquals(5, actual.getEntry().size());

        // Ensure the Observations were saved to the local server
        //Observation obs = ourClient.read().resource(Observation.class).withId("IdFromBundle").execute();

        //assertNotNull(obs);

        // Check other observation properties
    }

    @Test
    public void testMissingPeriodStartParam() throws IOException {

        Parameters params = new Parameters();
        params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        params.addParameter().setName("subject").setValue(new StringType("Patient/testReport01"));    

        assertThrows(InternalErrorException.class, () -> {
            ourClient.operation().onType(MeasureReport.class).named("$report")
            .withParameters(params)
            .returnResourceType(Parameters.class)
            .execute();
        });
    }

    @Test
    public void testMissingPeriodEndParam() throws IOException {

        Parameters params = new Parameters();
        params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        params.addParameter().setName("subject").setValue(new StringType("Patient/testReport01"));    

        assertThrows(InternalErrorException.class, () -> {
            ourClient.operation().onType(MeasureReport.class).named("$report")
            .withParameters(params)
            .returnResourceType(Parameters.class)
            .execute();
        });
    }

    @Test
    public void testMissingSubjectParam() throws IOException {

        Parameters params = new Parameters();
        params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));

        assertThrows(InternalErrorException.class, () -> {
            ourClient.operation().onType(MeasureReport.class).named("$report")
            .withParameters(params)
            .returnResourceType(Parameters.class)
            .execute();
        });
    }

    @Test
    public void testStartPeriodBeforeEndPeriod() throws IOException {

        Parameters params = new Parameters();
        params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        params.addParameter().setName("periodEnd").setValue(new StringType("2020-12-31"));

        assertThrows(InternalErrorException.class, () -> {
            ourClient.operation().onType(MeasureReport.class).named("$report")
            .withParameters(params)
            .returnResourceType(Parameters.class)
            .execute();
        });
    }

    @Test
    public void testSubjectIsPatientOrGroupd() throws IOException {

        Parameters patientSubjectParams = new Parameters();
        patientSubjectParams.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        patientSubjectParams.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        patientSubjectParams.addParameter().setName("subject").setValue(new StringType("Patient/testReport01")); 

        assertDoesNotThrow(() -> {
            ourClient.operation().onType(MeasureReport.class).named("$report")
            .withParameters(patientSubjectParams)
            .returnResourceType(Parameters.class)
            .execute();
        });

        Parameters groupSubjectParams = new Parameters();
        groupSubjectParams.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        groupSubjectParams.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        groupSubjectParams.addParameter().setName("subject").setValue(new StringType("Group/testReport01")); 

        assertDoesNotThrow(() -> {
            ourClient.operation().onType(MeasureReport.class).named("$report")
            .withParameters(groupSubjectParams)
            .returnResourceType(Parameters.class)
            .execute();
        });

        Parameters badSubjectParams = new Parameters();
        badSubjectParams.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        badSubjectParams.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        badSubjectParams.addParameter().setName("subject").setValue(new StringType("testReport01")); 

        assertThrows(InternalErrorException.class, () -> {
            ourClient.operation().onType(MeasureReport.class).named("$report")
            .withParameters(badSubjectParams)
            .returnResourceType(Parameters.class)
            .execute();
        });
    }
}
