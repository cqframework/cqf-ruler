package org.opencds.cqf.ruler.cr.r4.provider;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newPart;

import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CareGapsProviderIT.class,
		CrConfig.class }, properties = {
				"hapi.fhir.fhir_version=r4", "hapi.fhir.enforce_referential_integrity_on_write=false",
				"hapi.fhir.enforce_referential_integrity_on_delete=false", "hapi.fhir.cr.enabled=true",
				"hapi.fhir.cr.measure_report.care_gaps_reporter=Organization/alphora",
				"hapi.fhir.cr.measure_report.care_gaps_composition_section_author=Organization/alphora-author"
		})
public class CareGapsProviderIT extends RestIntegrationTest {

	private static final String periodStartValid = "2019-01-01";
	private static final String periodEndValid = "2019-12-31";
	private static final String subjectPatientValid = "Patient/numer-EXM125";
	private static final String subjectGroupValid = "Group/gic-gr-1";
	private static final String subjectGroupParallelValid = "Group/gic-gr-parallel";
	private static final String statusValid = "open-gap";
	private static final String statusValidSecond = "closed-gap";
	private static final String measureIdValid = "BreastCancerScreeningFHIR";
	private static final String measureUrlValid = "http://ecqi.healthit.gov/ecqms/Measure/BreastCancerScreeningFHIR";
	private static final String measureUrl = "http://ecqi.healthit.gov/ecqms/Measure/ColorectalCancerScreeningsFHIR";
	// TODO: spec probably needs to be updated to allow a full identifier, not just
	// a string
	private static final String measureIdentifierValid = "80366f35-e0a0-4ba7-a746-ad5760b79e01";
	private static final String practitionerValid = "gic-pra-1";
	private static final String organizationValid = "gic-org-1";
	private static final String dateInvalid = "bad-date";
	private static final String subjectInvalid = "bad-subject";
	private static final String statusInvalid = "bad-status";
	private static final String subjectReferenceInvalid = "Measure/gic-sub-1";

	@BeforeEach
	void beforeEach() throws Exception {
		loadResource("Alphora-organization.json");
		loadResource("AlphoraAuthor-organization.json");
		loadResource("numer-EXM125-patient.json");
	}

	private void beforeEachMeasure() throws Exception {
		loadTransaction("BreastCancerScreeningFHIR-bundle.json");
	}

	private void beforeMultiVersionMeasure() throws Exception {
		loadTransaction("ColorectalCancerScreeningsFHIR-bundle.json");
		loadResource("multiversion/Caregaps-Library-multiversion-manifest.json");
	}

	private void beforeEachParallelMeasure() throws Exception {
		loadResource("gic-gr-parallel.json");
		loadTransaction("BreastCancerScreeningFHIR-bundle.json");
	}

	private void beforeEachMultipleMeasures() throws Exception {
		loadTransaction("BreastCancerScreeningFHIR-bundle.json");
		loadTransaction("ColorectalCancerScreeningsFHIR-bundle.json");
	}

	@Test
	public void testMinimalParametersValid() throws Exception {
		beforeEachMeasure();

		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectPatientValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));

		assertDoesNotThrow(() -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@SuppressWarnings("java:S5778")
	@Test
	public void testPeriodStartNull() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectPatientValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@SuppressWarnings("java:S5778")
	@Test
	public void testPeriodStartInvalid() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(dateInvalid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectPatientValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@SuppressWarnings("java:S5778")
	@Test
	public void testPeriodEndNull() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectPatientValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@SuppressWarnings("java:S5778")
	@Test
	public void testPeriodEndInvalid() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(dateInvalid));
		params.addParameter().setName("subject").setValue(new StringType(subjectPatientValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@SuppressWarnings("java:S5778")
	@Test
	public void testPeriodInvalid() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodEndValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodStartValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectPatientValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@Test
	public void testSubjectGroupValid() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectGroupValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));

		loadResource("gic-gr-1.json");

		assertDoesNotThrow(() -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@SuppressWarnings("java:S5778")
	@Test
	public void testSubjectInvalid() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectInvalid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@SuppressWarnings("java:S5778")
	@Test
	public void testSubjectReferenceInvalid() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectReferenceInvalid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@SuppressWarnings("java:S5778")
	@Test
	public void testSubjectAndPractitioner() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectPatientValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));
		params.addParameter().setName("practitioner").setValue(new StringType(practitionerValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@SuppressWarnings("java:S5778")
	@Test
	public void testSubjectAndOrganization() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectPatientValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));
		params.addParameter().setName("organization").setValue(new StringType(organizationValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@Test
	public void testPractitionerAndOrganization() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));
		params.addParameter().setName("organization").setValue(new StringType(organizationValid));
		params.addParameter().setName("practitioner").setValue(new StringType(practitionerValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
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
	public void testOrganizationOnly() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));
		params.addParameter().setName("organization").setValue(new StringType(organizationValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
		// TODO: implement organization
		// assertDoesNotThrow(() -> {
		// getClient().operation().onType(Measure.class).named("$care-gaps")
		// .withParameters(params)
		// .useHttpGet()
		// .returnResourceType(Parameters.class)
		// .execute();
		// });
	}

	@SuppressWarnings("java:S5778")
	@Test
	public void testPractitionerOnly() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));
		params.addParameter().setName("practitioner").setValue(new StringType(practitionerValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@SuppressWarnings("java:S5778")
	@Test
	public void testSubjectMultiple() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectPatientValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectGroupValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@SuppressWarnings("java:S5778")
	@Test
	public void testNoMeasure() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectPatientValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@SuppressWarnings("java:S5778")
	@Test
	public void testStatusInvalid() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectPatientValid));
		params.addParameter().setName("status").setValue(new StringType(statusInvalid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@SuppressWarnings("java:S5778")
	@Test
	public void testStatusNull() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectPatientValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));

		assertThrows(InternalErrorException.class, () -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@Test
	public void testMultipleStatusValid() throws Exception {
		beforeEachMeasure();

		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectPatientValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("status").setValue(new StringType(statusValidSecond));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));

		assertDoesNotThrow(() -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@Test
	public void testMeasuresWithManifest() throws Exception {
		beforeMultiVersionMeasure();

		Parameters params = newParameters(
			newPart("periodStart", periodStartValid),
			newPart("periodEnd", periodEndValid),
			newPart("subject", subjectPatientValid),
			newPart("status", statusValid),
			newPart("status", statusValidSecond),
			newPart("measureUrl", measureUrl));

		Library library = getClient().read().resource(Library.class).withId("caregaps-library-multiversion-manifest").execute();
		assertNotNull(library);

		Parameters result = getClient().operation().onType(Measure.class).named("$care-gaps")
				.withParameters(params)
				.useHttpGet()
			   .withAdditionalHeader("X-Manifest", "http://alphora.com/fhir/Library/caregaps-library-multiversion-manifest")
				.returnResourceType(Parameters.class)
				.execute();

		assertNotNull(result);
	}

	@Test
	public void testMeasures() throws Exception {
		beforeEachMultipleMeasures();

		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectPatientValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("status").setValue(new StringType(statusValidSecond));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));
		// params.addParameter().setName("measureIdentifier")
		// .setValue(new StringType(measureIdentifierValid));
		params.addParameter().setName("measureUrl").setValue(new StringType(measureUrlValid));
		params.addParameter().setName("measureId").setValue(new StringType("ColorectalCancerScreeningsFHIR"));

		Parameters result = getClient().operation().onType(Measure.class).named("$care-gaps")
			.withParameters(params)
			.useHttpGet()
			.returnResourceType(Parameters.class)
			.execute();

		assertNotNull(result);
	}


	@SuppressWarnings("java:S5778")
	@Test
	public void testParallelMultiSubject() throws Exception {
		beforeEachParallelMeasure();

		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectGroupParallelValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));

		getClient().operation().onType(Measure.class).named("$care-gaps")
			.withParameters(params)
			.useHttpGet()
			.returnResourceType(Parameters.class)
			.execute();
	}
}
