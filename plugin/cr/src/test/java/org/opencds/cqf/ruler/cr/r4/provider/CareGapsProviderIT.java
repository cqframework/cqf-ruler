package org.opencds.cqf.ruler.cr.r4.provider;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.datePart;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.stringPart;

import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CareGapsProviderIT.class,
		CrConfig.class, CqlConfig.class }, properties = {
				"hapi.fhir.fhir_version=r4", "hapi.fhir.enforce_referential_integrity_on_write=false",
				"hapi.fhir.enforce_referential_integrity_on_delete=false", "hapi.fhir.cr.enabled=true",
				"hapi.fhir.cr.measure_report.care_gaps_reporter=Organization/alphora",
				"hapi.fhir.cr.measure_report.care_gaps_composition_section_author=Organization/alphora-author"
		})
class CareGapsProviderIT extends RestIntegrationTest {

	private static final String periodStartValid = "2019-01-01";
	private static final String periodEndValid = "2019-12-31";
	private static final String subjectPatientValid = "Patient/numer-EXM125";
	private static final String subjectGroupValid = "Group/gic-gr-1";
	private static final String subjectGroupParallelValid = "Group/gic-gr-parallel";
	private static final String statusValid = "open-gap";
	private static final String statusValidSecond = "closed-gap";
	private static final String measureIdValid = "BreastCancerScreeningFHIR";
	private static final String measureUrlValid = "http://ecqi.healthit.gov/ecqms/Measure/BreastCancerScreeningFHIR";
	private static final String practitionerValid = "gic-pra-1";
	private static final String organizationValid = "gic-org-1";
	private static final String dateInvalid = "bad-date";
	private static final String subjectInvalid = "bad-subject";
	private static final String statusInvalid = "bad-status";
	private static final String subjectReferenceInvalid = "Measure/gic-sub-1";

	@BeforeEach
	void beforeEach() {
		loadResource("Alphora-organization.json");
		loadResource("AlphoraAuthor-organization.json");
		loadResource("numer-EXM125-patient.json");
	}

	private void beforeEachMeasure() {
		loadTransaction("BreastCancerScreeningFHIR-bundle.json");
	}

	private void beforeEachParallelMeasure() {
		loadResource("gic-gr-parallel.json");
		loadTransaction("BreastCancerScreeningFHIR-bundle.json");
	}

	private void beforeEachMultipleMeasures() {
		loadTransaction("BreastCancerScreeningFHIR-bundle.json");
		loadTransaction("ColorectalCancerScreeningsFHIR-bundle.json");
	}

	@Test
	void testMinimalParametersValid() {
		beforeEachMeasure();

		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		assertDoesNotThrow(() -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@Test
	void testMinimalParametersValidPOST() {
		beforeEachMeasure();

		Parameters params = parameters(
				datePart("periodStart", periodStartValid),
				datePart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		assertDoesNotThrow(() -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@Test
	void testPeriodStartNull() {
		Parameters params = parameters(
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testPeriodStartNullPOST() {
		Parameters params = parameters(
				datePart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testPeriodStartInvalid() {
		Parameters params = parameters(
				stringPart("periodStart", dateInvalid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		IOperationUntypedWithInput<Parameters> request = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class);

		assertThrows(InvalidRequestException.class, request::execute);
	}

	@Test
	void testPeriodEndNull() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testPeriodEndNullPOST() {
		Parameters params = parameters(
				datePart("periodStart", periodStartValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testPeriodEndInvalid() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", dateInvalid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		IOperationUntypedWithInput<Parameters> request = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class);

		assertThrows(InvalidRequestException.class, request::execute);
	}

	@Test
	void testPeriodInvalid() {
		Parameters params = parameters(
				stringPart("periodStart", periodEndValid),
				stringPart("periodEnd", periodStartValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testPeriodInvalidPOST() {
		Parameters params = parameters(
				datePart("periodStart", periodEndValid),
				datePart("periodEnd", periodStartValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectGroupValid() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectGroupValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		loadResource("gic-gr-1.json");

		assertDoesNotThrow(() -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@Test
	void testSubjectGroupValidPOST() {
		Parameters params = parameters(
				datePart("periodStart", periodStartValid),
				datePart("periodEnd", periodEndValid),
				stringPart("subject", subjectGroupValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		loadResource("gic-gr-1.json");

		assertDoesNotThrow(() -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@Test
	void testSubjectInvalid() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectInvalid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectInvalidPOST() {
		Parameters params = parameters(
				datePart("periodStart", periodStartValid),
				datePart("periodEnd", periodEndValid),
				stringPart("subject", subjectInvalid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectReferenceInvalid() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectReferenceInvalid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectReferenceInvalidPOST() {
		Parameters params = parameters(
				datePart("periodStart", periodStartValid),
				datePart("periodEnd", periodEndValid),
				stringPart("subject", subjectReferenceInvalid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectAndPractitioner() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid),
				stringPart("practitioner", practitionerValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectAndPractitionerPOST() {
		Parameters params = parameters(
				datePart("periodStart", periodStartValid),
				datePart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid),
				stringPart("practitioner", practitionerValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectAndOrganization() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid),
				stringPart("organization", organizationValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectAndOrganizationPOST() {
		Parameters params = parameters(
				datePart("periodStart", periodStartValid),
				datePart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid),
				stringPart("organization", organizationValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testPractitionerAndOrganization() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid),
				stringPart("organization", organizationValid),
				stringPart("practitioner", practitionerValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Unsupported configuration"));

		// TODO: implement practitioner and organization
		// assertDoesNotThrow(() -> {
		// getClient().operation().onType(Measure.class).named("$care-gaps")
		// .withParameters(params)
		// .useHttpGet()
		// .returnResourceType(Parameters.class)
		// .execute();
		// });
	}

	@Test
	void testOrganizationOnly() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid),
				stringPart("organization", organizationValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Unsupported configuration"));

		// TODO: implement organization
		// assertDoesNotThrow(() -> {
		// getClient().operation().onType(Measure.class).named("$care-gaps")
		// .withParameters(params)
		// .useHttpGet()
		// .returnResourceType(Parameters.class)
		// .execute();
		// });
	}

	@Test
	void testPractitionerOnly() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid),
				stringPart("practitioner", practitionerValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testPractitionerOnlyPOST() {
		Parameters params = parameters(
				datePart("periodStart", periodStartValid),
				datePart("periodEnd", periodEndValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid),
				stringPart("practitioner", practitionerValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectMultiple() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("subject", subjectGroupValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testSubjectMultiplePOST() {
		Parameters params = parameters(
				datePart("periodStart", periodStartValid),
				datePart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("subject", subjectGroupValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testNoMeasure() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testNoMeasurePOST() {
		Parameters params = parameters(
				datePart("periodStart", periodStartValid),
				datePart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testStatusInvalid() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusInvalid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testStatusInvalidPOST() {
		Parameters params = parameters(
				datePart("periodStart", periodStartValid),
				datePart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusInvalid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testStatusNull() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testStatusNullPOST() {
		Parameters params = parameters(
				datePart("periodStart", periodStartValid),
				datePart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation()
				.onType(Measure.class).named("$care-gaps").withParameters(params)
				.returnResourceType(Parameters.class).execute();

		assertTrue(result.hasParameter("Invalid parameters"));
	}

	@Test
	void testMultipleStatusValid() {
		beforeEachMeasure();

		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("status", statusValidSecond),
				stringPart("measureId", measureIdValid));

		assertDoesNotThrow(() -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@Test
	void testMeasures() {
		beforeEachMultipleMeasures();

		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("status", statusValidSecond),
				stringPart("measureId", measureIdValid),
				stringPart("measureUrl", measureUrlValid),
				stringPart("measureId", "ColorectalCancerScreeningsFHIR"));

		Parameters result = getClient().operation().onType(Measure.class).named("$care-gaps")
				.withParameters(params)
				.useHttpGet()
				.returnResourceType(Parameters.class)
				.execute();

		assertNotNull(result);
	}

	@Test
	void testParallelMultiSubject() {
		beforeEachParallelMeasure();

		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectGroupParallelValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		Parameters result = getClient().operation().onType(Measure.class).named("$care-gaps")
				.withParameters(params)
				.useHttpGet()
				.returnResourceType(Parameters.class)
				.execute();

		assertNotNull(result);
	}
}
