package org.opencds.cqf.ruler.plugin.ra.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;

import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.ra.RAConfig;
import org.opencds.cqf.ruler.plugin.ra.RAProperties;
import org.opencds.cqf.ruler.plugin.utility.ClientUtilities;
import org.opencds.cqf.ruler.plugin.utility.ResolutionUtilities;
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
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
        RAConfig.class }, properties = { "hapi.fhir.fhir_version=r4", "hapi.fhir.ra.enabled=true" })
public class ReportProviderIT implements ClientUtilities, ResolutionUtilities {
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
        myRaProperties.getReport().setEndpoint(ourServerBase);
    }

    @AfterEach
    void afterEach() {
        ourClient.delete().resourceConditionalByUrl("Group?type=person").execute();
        ourClient.delete().resourceConditionalByUrl("Patient?active=true").execute();
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

    // TODO: add the count of patients returned
    @Test
    public void testSubjectPatient() throws IOException {

        Parameters params = new Parameters();
        params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        params.addParameter().setName("subject").setValue(new StringType("Patient/ra-patient01"));
        resolveByLocation(ourRegistry, "ra-patient01.json", ourCtx);

        assertDoesNotThrow(() -> {
            ourClient.operation().onType(MeasureReport.class).named("$report")
                    .withParameters(params)
                    .returnResourceType(Parameters.class)
                    .execute();
        });
    }

    // TODO: add the count of patients returned
    @Test
    public void testSubjectGroup() throws IOException {

        Parameters params = new Parameters();
        params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        params.addParameter().setName("subject").setValue(new StringType("Group/ra-group01"));
        resolveByLocation(ourRegistry, "ra-patient01.json", ourCtx);
        resolveByLocation(ourRegistry, "ra-group01.json", ourCtx);

        assertDoesNotThrow(() -> {
            ourClient.operation().onType(MeasureReport.class).named("$report")
                    .withParameters(params)
                    .returnResourceType(Parameters.class)
                    .execute();
        });
    }

    @Test
    public void testSubjectIsNotPatientOrGroup() throws IOException {

        Parameters params = new Parameters();
        params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        params.addParameter().setName("subject").setValue(new StringType("ra-patient01"));

        assertThrows(InternalErrorException.class, () -> {
            ourClient.operation().onType(MeasureReport.class).named("$report")
                    .withParameters(params)
                    .returnResourceType(Parameters.class)
                    .execute();
        });
    }

    @Test
    public void testPatientSubjectNotFound() throws IOException {

        Parameters params = new Parameters();
        params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        params.addParameter().setName("subject").setValue(new StringType("Patient/bad-patient"));

        assertThrows(ResourceNotFoundException.class, () -> {
            ourClient.operation().onType(MeasureReport.class).named("$report")
                    .withParameters(params)
                    .returnResourceType(Parameters.class)
                    .execute();
        });
    }

    @Test
    public void testGroupSubjectNotFound() throws IOException {

        Parameters params = new Parameters();
        params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        params.addParameter().setName("subject").setValue(new StringType("Group/bad-group"));

        assertThrows(ResourceNotFoundException.class, () -> {
            ourClient.operation().onType(MeasureReport.class).named("$report")
                    .withParameters(params)
                    .returnResourceType(Parameters.class)
                    .execute();
        });
    }

    // This test requires the following application setting:
    // enforce_referential_integrity_on_write: false
    @Test
    public void testSubjectPatientNotFoundInGroup() throws IOException {

        Parameters params = new Parameters();
        params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        params.addParameter().setName("subject").setValue(new StringType("Group/ra-group01"));
        resolveByLocation(ourRegistry, "ra-group01.json", ourCtx);
        Group group = ourClient.read().resource(Group.class).withId("ra-group01").execute();
        assertNotNull(group);

        assertThrows(ResourceNotFoundException.class, () -> {
            ourClient.operation().onType(MeasureReport.class).named("$report")
                    .withParameters(params)
                    .returnResourceType(Parameters.class)
                    .execute();
        });
    }

    // TODO: add the count of patients returned
    @Test
    public void testSubjectMultiplePatientGroup() throws IOException {

        Parameters params = new Parameters();
        params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        params.addParameter().setName("subject").setValue(new StringType("Group/ra-group02"));
        resolveByLocation(ourRegistry, "ra-patient02.json", ourCtx);
        resolveByLocation(ourRegistry, "ra-patient03.json", ourCtx);
        resolveByLocation(ourRegistry, "ra-group02.json", ourCtx);

        assertDoesNotThrow(() -> {
            ourClient.operation().onType(MeasureReport.class).named("$report")
                    .withParameters(params)
                    .returnResourceType(Parameters.class)
                    .execute();
        });
    }

    @Test
    public void testReport() throws IOException {

        // QuestionnaireResponse test =
        // (QuestionnaireResponse)ourCtx.newJsonParser().parseResource(stringFromResource("mypain-questionnaire-response.json"));

        Parameters params = new Parameters();
        params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        params.addParameter().setName("subject").setValue(new StringType("Patient/ra-patient01"));
        resolveByLocation(ourRegistry, "ra-patient01.json", ourCtx);

        Parameters actual = ourClient.operation().onType(MeasureReport.class).named("$report")
                .withParameters(params)
                .returnResourceType(Parameters.class)
                .execute();

        assertNotNull(actual);

    }
}
