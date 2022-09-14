package org.opencds.cqf.ruler.ra.r4;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.ra.RAConfig;
import org.opencds.cqf.ruler.ra.RAProperties;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.opencds.cqf.ruler.test.utility.Urls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newPart;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = { ReportProviderIT.class, RAConfig.class },
		properties = { "hapi.fhir.fhir_version=r4" })
class ReportProviderIT extends RestIntegrationTest {
	@Autowired
	private RAProperties myRaProperties;

	@BeforeEach
	public void beforeEach() {
		String ourServerBase = Urls.getUrl(myRaProperties.getReport().getEndpoint(), getPort());
		myRaProperties.getReport().setEndpoint(ourServerBase);
	}

	@Test
	void testMissingPeriodStartParam() {
		Parameters params = newParameters(
				newPart("periodEnd", new StringType("2021-12-31")),
				newPart("subject", new StringType("Patient/testReport01")),
				newPart("measureId", new StringType("Measure-RAModelExample01")));

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testMissingPeriodEndParam() {
		Parameters params = newParameters(
				newPart("periodStart", new StringType("2021-01-01")),
				newPart("subject", new StringType("Patient/testReport01")),
				newPart("measureId", new StringType("Measure-RAModelExample01")));

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testMissingSubjectParam() {
		Parameters params = newParameters(
				newPart("periodStart", new StringType("2021-01-01")),
				newPart("periodEnd", new StringType("2021-12-31")),
				newPart("measureId", new StringType("Measure-RAModelExample01")));

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testStartPeriodBeforeEndPeriod() {
		Parameters params = newParameters(
				newPart("periodStart", new StringType("2021-01-01")),
				newPart("periodEnd", new StringType("2020-12-31")),
				newPart("subject", new StringType("Patient/testReport01")),
				newPart("measureId", new StringType("Measure-RAModelExample01")));

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	// TODO: add the count of patients returned
	@Test
	void testSubjectPatient() {
		Parameters params = newParameters(
				newPart("periodStart", new StringType("2021-01-01")),
				newPart("periodEnd", new StringType("2021-12-31")),
				newPart("subject", new StringType("Patient/ra-patient01")),
				newPart("measureId", new StringType("Measure-RAModelExample01")));

		loadResource("Patient-ra-patient01.json");

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertFalse(result.hasParameter("Invalid parameters"));
	}

	// TODO: add the count of patients returned
	@Test
	void testSubjectGroup() {
		Parameters params = newParameters(
				newPart("periodStart", new StringType("2021-01-01")),
				newPart("periodEnd", new StringType("2021-12-31")),
				newPart("subject", new StringType("Group/ra-group01")),
				newPart("measureId", new StringType("Measure-RAModelExample01")));

		loadResource("Patient-ra-patient01.json");
		loadResource("Group-ra-group01.json");

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertFalse(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectIsNotPatientOrGroup() {
		Parameters params = newParameters(
				newPart("periodStart", new StringType("2021-01-01")),
				newPart("periodEnd", new StringType("2021-12-31")),
				newPart("subject", new StringType("ra-patient01")),
				newPart("measureId", new StringType("Measure-RAModelExample01")));

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testPatientSubjectNotFound() {
		Parameters params = newParameters(
				newPart("periodStart", new StringType("2021-01-01")),
				newPart("periodEnd", new StringType("2021-12-31")),
				newPart("subject", new StringType("Patient/bad-patient")),
				newPart("measureId", new StringType("Measure-RAModelExample01")));

		assertThrows(ResourceNotFoundException.class, () -> getClient().operation()
				.onType(MeasureReport.class).named("$report").withParameters(params)
				.returnResourceType(Parameters.class).execute());
	}

	@Test
	void testGroupSubjectNotFound() {
		Parameters params = newParameters(
				newPart("periodStart", new StringType("2021-01-01")),
				newPart("periodEnd", new StringType("2021-12-31")),
				newPart("subject", new StringType("Group/bad-group")),
				newPart("measureId", new StringType("Measure-RAModelExample01")));

		assertThrows(ResourceNotFoundException.class, () -> getClient().operation()
				.onType(MeasureReport.class).named("$report").withParameters(params)
				.returnResourceType(Parameters.class).execute());
	}

	// This test requires the following application setting:
	// enforce_referential_integrity_on_write: false
	@Test
	void testSubjectPatientNotFoundInGroup() {
		Parameters params = newParameters(
				newPart("periodStart", new StringType("2021-01-01")),
				newPart("periodEnd", new StringType("2021-12-31")),
				newPart("subject", new StringType("Group/ra-group00")),
				newPart("measureId", new StringType("Measure-RAModelExample01")));

		loadResource("Group-ra-group00.json");
		Group group = getClient().read().resource(Group.class).withId("ra-group00").execute();
		assertNotNull(group);

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertFalse(result.hasParameter("Invalid parameters"));
	}

	// TODO: add the count of patients returned
	@Test
	void testSubjectMultiplePatientGroup() {
		Parameters params = newParameters(
				newPart("periodStart", new StringType("2021-01-01")),
				newPart("periodEnd", new StringType("2021-12-31")),
				newPart("subject", new StringType("Group/ra-group02")),
				newPart("measureId", new StringType("Measure-RAModelExample01")));

		loadResource("Patient-ra-patient02.json");
		loadResource("Patient-ra-patient03.json");
		loadResource("Group-ra-group02.json");

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertFalse(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSingleSubjectSingleReport() {
		Parameters params = newParameters(
				newPart("periodStart", new StringType("2021-01-01")),
				newPart("periodEnd", new StringType("2021-12-31")),
				newPart("subject", new StringType("Patient/ra-patient01")),
				newPart("measureId", new StringType("Measure-RAModelExample01")));

		loadResource("Patient-ra-patient01.json");
		loadResource("Condition-ra-condition02pat01.json");
		loadResource("Condition-ra-condition03pat01.json");
		loadResource("Condition-ra-condition08pat01.json");
		loadResource("Condition-ra-condition09pat01.json");
		loadResource("Condition-ra-condition10pat01.json");
		loadResource("Condition-ra-condition11pat01.json");
		loadResource("Condition-ra-condition17pat01.json");
		loadResource("Condition-ra-condition18pat01.json");
		loadResource("Condition-ra-condition33pat01.json");
		loadResource("Condition-ra-condition43pat01.json");
		loadResource("Condition-ra-condition44pat01.json");
		loadResource("Observation-ra-obs21pat01.json");
		loadResource("Encounter-ra-encounter02pat01.json");
		loadResource("Encounter-ra-encounter03pat01.json");
		loadResource("Encounter-ra-encounter08pat01.json");
		loadResource("Encounter-ra-encounter09pat01.json");
		loadResource("Encounter-ra-encounter11pat01.json");
		loadResource("Encounter-ra-encounter43pat01.json");
		loadResource("Encounter-ra-encounter44pat01.json");
		loadResource("MeasureReport-ra-measurereport01.json");

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertNotNull(result);
		assertEquals(1, result.getParameter().size());

		Bundle bundle = (Bundle) result.getParameter().get(0).getResource();
		assertNotNull(bundle);
		// all the resources inserted above are in the bundle entry
		assertEquals(21, bundle.getEntry().size());
	}

	@Test
	void testReportDoesNotIncludeNonEvaluatedResources() {
		Parameters params = newParameters(
				newPart("periodStart", new StringType("2021-01-01")),
				newPart("periodEnd", new StringType("2021-12-31")),
				newPart("subject", new StringType("Patient/ra-patient01")),
				newPart("measureId", new StringType("Measure-RAModelExample01")));

		loadResource("Patient-ra-patient01.json");
		loadResource("Condition-ra-condition02pat01.json");
		loadResource("Condition-ra-condition03pat01.json");
		loadResource("Condition-ra-condition08pat01.json");
		loadResource("Condition-ra-condition09pat01.json");
		loadResource("Condition-ra-condition10pat01.json");
		loadResource("Condition-ra-condition11pat01.json");
		loadResource("Condition-ra-condition17pat01.json");
		loadResource("Condition-ra-condition18pat01.json");
		loadResource("Condition-ra-condition33pat01.json");
		loadResource("Condition-ra-condition43pat01.json");
		loadResource("Condition-ra-condition44pat01.json");
		loadResource("Observation-ra-obs21pat01.json");
		loadResource("Encounter-ra-encounter02pat01.json");
		loadResource("Encounter-ra-encounter03pat01.json");
		loadResource("Encounter-ra-encounter08pat01.json");
		loadResource("Encounter-ra-encounter09pat01.json");
		loadResource("Encounter-ra-encounter11pat01.json");
		loadResource("Encounter-ra-encounter43pat01.json");
		loadResource("Encounter-ra-encounter44pat01.json");
		loadResource("MeasureReport-ra-measurereport01.json");
		// this is not an evaluatedResource of the report
		loadResource("Encounter-ra-encounter45pat01.json");

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		Bundle bundle = (Bundle) result.getParameter().get(0).getResource();
		// all the resources inserted above are in the bundle entry except the one that
		// was not evaluated
		assertEquals(21, bundle.getEntry().size());
	}

	// TODO: create test for single patient, multiple reports
	// TODO: create test for multiple patients, multiple reports
	// TODO: create tests of overlap of MeasureReport date and period
}
