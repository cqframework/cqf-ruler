package org.opencds.cqf.ruler.cr.r4.provider;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CareGapsProviderIT.class,
		CrConfig.class }, properties = {
				"hapi.fhir.fhir_version=r4",
		})
public class CareGapsProviderIT extends RestIntegrationTest {

	private static final String periodStartValid = "2022-01-01";
	private static final String periodEndValid = "2022-01-15";
	private static final String subjectPatientValid = "Patient/12345";
	private static final String subjectGroupValid = "Group/12345";
	private static final String statusValid = "open-gap";
	private static final String statusValidSecond = "closed-gap";
	private static final String measureIdValid = "measure-EXM130-7.3.000";
	private static final String practitionerValid = "12345";
	private static final String organizationValid = "12345";
	private static final String dateInvalid = "bad-date";
	private static final String subjectInvalid = "bad-subject";
	private static final String statusInvalid = "bad-status";
	private static final String subjectReferenceInvalid = "Measure/12345";

	@Test
	public void testMinimalParametersValid() throws Exception {
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

		assertDoesNotThrow(() -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}

	@Test
	public void testOrganizationOnly() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType(measureIdValid));
		params.addParameter().setName("organization").setValue(new StringType(organizationValid));

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
	public void testMeasures() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectPatientValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));
		params.addParameter().setName("measureId").setValue(new StringType("CervicalCancerScreeningFHIR"));
		params.addParameter().setName("measureIdentifier")
				.setValue(new StringType("2138c351-1c17-4298-aebc-43b42b1aa1ba"));
		params.addParameter().setName("measureUrl")
				.setValue(new StringType("http://ecqi.healthit.gov/ecqms/Measure/CervicalCancerScreeningFHIR"));

		params.addParameter().setName("measureId").setValue(new StringType("ColorectalCancerScreeningsFHIR"));

		loadResource("CervicalCancerScreeningFHIR.json");
		loadResource("ColorectalCancerScreeningsFHIR.json");

		assertDoesNotThrow(() -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}
}
