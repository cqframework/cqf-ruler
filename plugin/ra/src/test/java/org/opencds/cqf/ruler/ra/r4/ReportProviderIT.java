package org.opencds.cqf.ruler.ra.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.ruler.utility.r4.Parameters.datePart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.parameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.stringPart;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.ra.RAConfig;
import org.opencds.cqf.ruler.ra.RAConstants;
import org.opencds.cqf.ruler.ra.RAProperties;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.opencds.cqf.ruler.test.utility.Urls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { ReportProviderIT.class,
		RAConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
class ReportProviderIT extends RestIntegrationTest {
	@Autowired
	private RAProperties myRaProperties;

	@BeforeEach
	public void beforeEach() {
		String ourServerBase = Urls.getUrl(myRaProperties.getReport().getEndpoint(), getPort());
		myRaProperties.getReport().setEndpoint(ourServerBase);
	}

	@Test
	void testMissingPeriodStartParamGET() {
		Parameters params = parameters(
				stringPart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Patient/testReport01"));

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testMissingPeriodStartParamPOST() {
		Parameters params = parameters(
				datePart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Patient/testReport01"));

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testMissingPeriodEndParamGET() {
		Parameters params = parameters(
				stringPart(RAConstants.PERIOD_START, "2021-01-01"),
				stringPart(RAConstants.SUBJECT, "Patient/testReport01"));

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testMissingPeriodEndParamPOST() {
		Parameters params = parameters(
				datePart(RAConstants.PERIOD_START, "2021-01-01"),
				stringPart(RAConstants.SUBJECT, "Patient/testReport01"));

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testInvalidStartPeriodParamGET() {
		Parameters params = parameters(
				stringPart(RAConstants.PERIOD_START, "2021/01/01"),
				stringPart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Patient/testReport01"));

		IOperationUntypedWithInput<Parameters> request = getClient().operation()
				.onType(MeasureReport.class).named("$ra.report").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class);
		assertThrows(InvalidRequestException.class, request::execute);
	}

	@Test
	void testInvalidEndPeriodParamGET() {
		Parameters params = parameters(
				stringPart(RAConstants.PERIOD_START, "2021-01-01"),
				stringPart(RAConstants.PERIOD_END, "2021/12/31"),
				stringPart(RAConstants.SUBJECT, "Patient/testReport01"));

		IOperationUntypedWithInput<Parameters> request = getClient().operation()
				.onType(MeasureReport.class).named("$ra.report").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class);
		assertThrows(InvalidRequestException.class, request::execute);
	}

	@Test
	void testMissingSubjectParamGET() {
		Parameters params = parameters(
				stringPart(RAConstants.PERIOD_START, "2021-01-01"),
				stringPart(RAConstants.PERIOD_END, "2021-12-31"));

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter(RAConstants.INVALID_PARAMETERS_NAME));
	}

	@Test
	void testMissingSubjectParamPOST() {
		Parameters params = parameters(
				datePart(RAConstants.PERIOD_START, "2021-01-01"),
				datePart(RAConstants.PERIOD_END, "2021-12-31"));

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter(RAConstants.INVALID_PARAMETERS_NAME));
	}

	@Test
	void testEndPeriodBeforeStartPeriodGET() {
		Parameters params = parameters(
				stringPart(RAConstants.PERIOD_START, "2021-01-01"),
				stringPart(RAConstants.PERIOD_END, "2020-12-31"),
				stringPart(RAConstants.SUBJECT, "Patient/testReport01"));

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter(RAConstants.INVALID_PARAMETERS_NAME));
	}

	@Test
	void testEndPeriodBeforeStartPeriodPOST() {
		Parameters params = parameters(
				datePart(RAConstants.PERIOD_START, "2021-01-01"),
				datePart(RAConstants.PERIOD_END, "2020-12-31"),
				stringPart(RAConstants.SUBJECT, "Patient/testReport01"));

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter(RAConstants.INVALID_PARAMETERS_NAME));
	}

	@Test
	void testSubjectPatientGET() {
		Parameters params = parameters(
				stringPart(RAConstants.PERIOD_START, "2021-01-01"),
				stringPart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Patient/ra-patient01"));

		loadResource("Patient-ra-patient01.json");

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertFalse(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectPatientPOST() {
		Parameters params = parameters(
				datePart(RAConstants.PERIOD_START, "2021-01-01"),
				datePart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Patient/ra-patient01"));

		loadResource("Patient-ra-patient01.json");

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).returnResourceType(Parameters.class).execute();

		assertFalse(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectGroupGET() {
		Parameters params = parameters(
				stringPart(RAConstants.PERIOD_START, "2021-01-01"),
				stringPart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Group/ra-group01"));

		loadResource("Patient-ra-patient01.json");
		loadResource("Group-ra-group01.json");

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertFalse(result.hasParameter("Invalid parameters"));
		assertEquals(1, result.getParameter().size());
	}

	@Test
	void testSubjectGroupPOST() {
		Parameters params = parameters(
				datePart(RAConstants.PERIOD_START, "2021-01-01"),
				datePart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Group/ra-group01"));

		loadResource("Patient-ra-patient01.json");
		loadResource("Group-ra-group01.json");

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).returnResourceType(Parameters.class).execute();

		assertFalse(result.hasParameter("Invalid parameters"));
		assertEquals(1, result.getParameter().size());
	}

	@Test
	void testSubjectIsNotPatientOrGroupGET() {
		Parameters params = parameters(
				stringPart(RAConstants.PERIOD_START, "2021-01-01"),
				stringPart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "ra-patient01"));

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectIsNotPatientOrGroupPOST() {
		Parameters params = parameters(
				datePart(RAConstants.PERIOD_START, "2021-01-01"),
				datePart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "ra-patient01"));

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testPatientSubjectNotFoundGET() {
		Parameters params = parameters(
				stringPart(RAConstants.PERIOD_START, "2021-01-01"),
				stringPart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Patient/bad-patient"));

		IOperationUntypedWithInput<Parameters> request = getClient().operation()
				.onType(MeasureReport.class).named("$ra.report").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class);
		assertThrows(ResourceNotFoundException.class, request::execute);
	}

	@Test
	void testPatientSubjectNotFoundPOST() {
		Parameters params = parameters(
				datePart(RAConstants.PERIOD_START, "2021-01-01"),
				datePart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Patient/bad-patient"));

		IOperationUntypedWithInput<Parameters> request = getClient().operation()
				.onType(MeasureReport.class).named("$ra.report").withParameters(params)
				.returnResourceType(Parameters.class);
		assertThrows(ResourceNotFoundException.class, request::execute);
	}

	@Test
	void testGroupSubjectNotFoundGET() {
		Parameters params = parameters(
				stringPart(RAConstants.PERIOD_START, "2021-01-01"),
				stringPart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Group/bad-group"));

		IOperationUntypedWithInput<Parameters> request = getClient().operation()
				.onType(MeasureReport.class).named("$ra.report").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class);
		assertThrows(ResourceNotFoundException.class, request::execute);
	}

	@Test
	void testGroupSubjectNotFoundPOST() {
		Parameters params = parameters(
				datePart(RAConstants.PERIOD_START, "2021-01-01"),
				datePart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Group/bad-group"));

		IOperationUntypedWithInput<Parameters> request = getClient().operation()
				.onType(MeasureReport.class).named("$ra.report").withParameters(params)
				.returnResourceType(Parameters.class);
		assertThrows(ResourceNotFoundException.class, request::execute);
	}

	// This test requires the following application setting:
	// enforce_referential_integrity_on_write: false
	@Test
	void testSubjectPatientNotFoundInGroupGET() {
		Parameters params = parameters(
				stringPart(RAConstants.PERIOD_START, "2021-01-01"),
				stringPart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Group/ra-group00"));

		loadResource("Group-ra-group00.json");
		Group group = getClient().read().resource(Group.class).withId("ra-group00").execute();
		assertNotNull(group);

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertFalse(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectPatientNotFoundInGroupPOST() {
		Parameters params = parameters(
				datePart(RAConstants.PERIOD_START, "2021-01-01"),
				datePart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Group/ra-group00"));

		loadResource("Group-ra-group00.json");
		Group group = getClient().read().resource(Group.class).withId("ra-group00").execute();
		assertNotNull(group);

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).returnResourceType(Parameters.class).execute();

		assertFalse(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectMultiplePatientGroupGET() {
		Parameters params = parameters(
				stringPart(RAConstants.PERIOD_START, "2021-01-01"),
				stringPart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Group/ra-group02"));

		loadResource("Patient-ra-patient02.json");
		loadResource("Patient-ra-patient03.json");
		loadResource("Group-ra-group02.json");

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertFalse(result.hasParameter("Invalid parameters"));
		assertEquals(2, result.getParameter().size());
	}

	@Test
	void testSubjectMultiplePatientGroupPOST() {
		Parameters params = parameters(
				datePart(RAConstants.PERIOD_START, "2021-01-01"),
				datePart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Group/ra-group02"));

		loadResource("Patient-ra-patient02.json");
		loadResource("Patient-ra-patient03.json");
		loadResource("Group-ra-group02.json");

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).returnResourceType(Parameters.class).execute();

		assertFalse(result.hasParameter("Invalid parameters"));
		assertEquals(2, result.getParameter().size());
	}

	@Test
	void testSingleSubjectSingleReportGET() {
		Parameters params = parameters(
				stringPart(RAConstants.PERIOD_START, "2021-01-01"),
				stringPart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Patient/ra-patient01"));

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

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		assertNotNull(result);
		assertEquals(1, result.getParameter().size());

		Bundle bundle = (Bundle) result.getParameter().get(0).getResource();
		assertNotNull(bundle);
		// all the resources inserted above are in the bundle entry
		assertEquals(21, bundle.getEntry().size());
	}

	@Test
	void testSingleSubjectSingleReportPOST() {
		Parameters params = parameters(
				datePart(RAConstants.PERIOD_START, "2021-01-01"),
				datePart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Patient/ra-patient01"));

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

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).returnResourceType(Parameters.class).execute();

		assertNotNull(result);
		assertEquals(1, result.getParameter().size());

		Bundle bundle = (Bundle) result.getParameter().get(0).getResource();
		assertNotNull(bundle);
		// all the resources inserted above are in the bundle entry
		assertEquals(21, bundle.getEntry().size());
	}

	@Test
	void testReportDoesNotIncludeNonEvaluatedResourcesGET() {
		Parameters params = parameters(
				stringPart(RAConstants.PERIOD_START, "2021-01-01"),
				stringPart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Patient/ra-patient01"));

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

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).useHttpGet().returnResourceType(Parameters.class).execute();

		Bundle bundle = (Bundle) result.getParameter().get(0).getResource();
		// all the resources inserted above are in the bundle entry except the one that
		// was not evaluated
		assertEquals(21, bundle.getEntry().size());
	}

	@Test
	void testReportDoesNotIncludeNonEvaluatedResourcesPOST() {
		Parameters params = parameters(
				datePart(RAConstants.PERIOD_START, "2021-01-01"),
				datePart(RAConstants.PERIOD_END, "2021-12-31"),
				stringPart(RAConstants.SUBJECT, "Patient/ra-patient01"));

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

		Parameters result = getClient().operation().onType(MeasureReport.class).named("$ra.report")
				.withParameters(params).returnResourceType(Parameters.class).execute();

		Bundle bundle = (Bundle) result.getParameter().get(0).getResource();
		// all the resources inserted above are in the bundle entry except the one that
		// was not evaluated
		assertEquals(21, bundle.getEntry().size());
	}

	// TODO: create test for single patient, multiple reports
	// TODO: create test for multiple patients, multiple reports
	// TODO: create tests of overlap of MeasureReport date and period
}
