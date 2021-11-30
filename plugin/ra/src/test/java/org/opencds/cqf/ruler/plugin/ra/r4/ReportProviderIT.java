package org.opencds.cqf.ruler.plugin.ra.r4;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.ra.RAConfig;
import org.opencds.cqf.ruler.plugin.ra.RAProperties;
import org.opencds.cqf.ruler.plugin.testutility.ResolutionUtilities;
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
public class ReportProviderIT implements org.opencds.cqf.ruler.plugin.testutility.ClientUtilities, ResolutionUtilities, org.opencds.cqf.ruler.plugin.utility.ClientUtilities {
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
        resolveByLocation(ourRegistry, "Patient-ra-patient01.json", ourCtx);

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
        resolveByLocation(ourRegistry, "Patient-ra-patient01.json", ourCtx);
        resolveByLocation(ourRegistry, "Group-ra-group01.json", ourCtx);

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
        params.addParameter().setName("subject").setValue(new StringType("Group/ra-group00"));
        resolveByLocation(ourRegistry, "Group-ra-group00.json", ourCtx);
        Group group = ourClient.read().resource(Group.class).withId("ra-group00").execute();
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
        resolveByLocation(ourRegistry, "Patient-ra-patient02.json", ourCtx);
        resolveByLocation(ourRegistry, "Patient-ra-patient03.json", ourCtx);
        resolveByLocation(ourRegistry, "Group-ra-group02.json", ourCtx);

        assertDoesNotThrow(() -> {
            ourClient.operation().onType(MeasureReport.class).named("$report")
                    .withParameters(params)
                    .returnResourceType(Parameters.class)
                    .execute();
        });
    }

    @Test
    public void testSingleSubjectSingleReport() throws IOException {

        Parameters params = new Parameters();
        params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        params.addParameter().setName("subject").setValue(new StringType("Patient/ra-patient01"));
        resolveByLocation(ourRegistry, "Patient-ra-patient01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition02pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition03pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition08pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition09pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition10pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition11pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition17pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition18pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition33pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition43pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition44pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Observation-ra-obs21pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Encounter-ra-encounter02pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Encounter-ra-encounter03pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Encounter-ra-encounter08pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Encounter-ra-encounter09pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Encounter-ra-encounter11pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Encounter-ra-encounter43pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Encounter-ra-encounter44pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "MeasureReport-ra-measurereport01.json", ourCtx);

        Parameters actual = ourClient.operation().onType(MeasureReport.class).named("$report")
                .withParameters(params)
                .returnResourceType(Parameters.class)
                .execute();

        assertNotNull(actual);
        assertEquals(1, actual.getParameter().size());

        Bundle bundle = (Bundle) actual.getParameter().get(0).getResource();
        assertNotNull(bundle);
        // all the resources inserted above are in the bundle entry
        assertEquals(21, bundle.getEntry().size());
    }

    @Test
    public void testReportDoesNotIncludeNonEvaluatedResources() throws IOException {

        Parameters params = new Parameters();
        params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
        params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
        params.addParameter().setName("subject").setValue(new StringType("Patient/ra-patient01"));
        resolveByLocation(ourRegistry, "Patient-ra-patient01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition02pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition03pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition08pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition09pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition10pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition11pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition17pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition18pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition33pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition43pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Condition-ra-condition44pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Observation-ra-obs21pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Encounter-ra-encounter02pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Encounter-ra-encounter03pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Encounter-ra-encounter08pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Encounter-ra-encounter09pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Encounter-ra-encounter11pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Encounter-ra-encounter43pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "Encounter-ra-encounter44pat01.json", ourCtx);
        resolveByLocation(ourRegistry, "MeasureReport-ra-measurereport01.json", ourCtx);
        // this is not an evaluatedResource of the report
        resolveByLocation(ourRegistry, "Encounter-ra-encounter45pat01.json", ourCtx);

        Parameters actual = ourClient.operation().onType(MeasureReport.class).named("$report")
                .withParameters(params)
                .returnResourceType(Parameters.class)
                .execute();

        Bundle bundle = (Bundle) actual.getParameter().get(0).getResource();
        // all the resources inserted above are in the bundle entry except the one that
        // was not evaluated
        assertEquals(21, bundle.getEntry().size());
    }

    // TODO: create test for single patient, multiple reports
    // TODO: create test for multiple patients, multiple reports
    // TODO: create tests of overlap of MeasureReport date and period
}
