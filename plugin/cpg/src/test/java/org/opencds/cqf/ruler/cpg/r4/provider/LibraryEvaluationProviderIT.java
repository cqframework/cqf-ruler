package org.opencds.cqf.ruler.cpg.r4.provider;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cpg.CpgConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = { LibraryEvaluationProviderIT.class, CpgConfig.class },
		properties = { "hapi.fhir.fhir_version=r4" })
class LibraryEvaluationProviderIT extends RestIntegrationTest {

	private final String packagePrefix = "org/opencds/cqf/ruler/cpg/r4/provider/";

	@Test
	void testSimpleAsthmaInlineCode() {
		loadResource(packagePrefix + "SimplePatient.json");
		loadResource(packagePrefix + "SimpleCondition.json");
		loadResource(packagePrefix + "AsthmaTest.json");
		Parameters params = org.opencds.cqf.ruler.utility.r4.Parameters.newParameters(
			org.opencds.cqf.ruler.utility.r4.Parameters.newPart(
					"subject", new StringType("Patient/SimplePatient"))
		);
		Parameters result = getClient().operation()
			.onInstance(new IdType("Library", "AsthmaTest"))
			.named("$evaluate")
			.withParameters(params)
			.returnResourceType(Parameters.class)
			.execute();

		assertNotNull(result);
		assertTrue(result.hasParameter("Has Asthma Diagnosis"));
		assertTrue(((BooleanType) result.getParameter("Has Asthma Diagnosis")).booleanValue());
	}

	@Test
	void testSimpleLibrary() {
		loadResource(packagePrefix + "SimplePatient.json");
		loadResource(packagePrefix + "SimpleObservation.json");
		loadResource(packagePrefix + "SimpleR4Library.json");
		Parameters params = org.opencds.cqf.ruler.utility.r4.Parameters.newParameters(
			org.opencds.cqf.ruler.utility.r4.Parameters.newPart(
					"subject", new StringType("Patient/SimplePatient"))
		);
		Parameters result = getClient().operation()
			.onInstance(new IdType("Library", "SimpleR4Library"))
			.named("$evaluate")
			.withParameters(params)
			.returnResourceType(Parameters.class)
			.execute();

		assertNotNull(result);
		assertTrue(result.hasParameter("Initial Population"));
		assertTrue(((BooleanType) result.getParameter("Initial Population")).booleanValue());
		assertTrue(result.hasParameter("Numerator"));
		assertTrue(((BooleanType) result.getParameter("Numerator")).booleanValue());
		assertTrue(result.hasParameter("Denominator"));
		assertTrue(((BooleanType) result.getParameter("Denominator")).booleanValue());
	}

	@Test
	void testSimpleLibraryWithBundle() {
		loadResource(packagePrefix + "SimpleR4Library.json");
		Bundle data = (Bundle) loadResource(packagePrefix + "SimpleDataBundle.json");
		Parameters params = org.opencds.cqf.ruler.utility.r4.Parameters.newParameters(
			org.opencds.cqf.ruler.utility.r4.Parameters.newPart("subject", "SimplePatient"),
			org.opencds.cqf.ruler.utility.r4.Parameters.newPart("data", data),
			org.opencds.cqf.ruler.utility.r4.Parameters.newPart(
					"useServerData", new BooleanType(false))
		);
		Parameters result = getClient().operation()
			.onInstance(new IdType("Library", "SimpleR4Library"))
			.named("$evaluate")
			.withParameters(params)
			.returnResourceType(Parameters.class)
			.execute();

		assertNotNull(result);
		assertTrue(result.hasParameter("Initial Population"));
		assertTrue(((BooleanType) result.getParameter("Initial Population")).booleanValue());
		assertTrue(result.hasParameter("Numerator"));
		assertTrue(((BooleanType) result.getParameter("Numerator")).booleanValue());
		assertTrue(result.hasParameter("Denominator"));
		assertTrue(((BooleanType) result.getParameter("Denominator")).booleanValue());
	}

	@Test
	void testOpioidRec10Library() {
		loadTransaction(packagePrefix + "OpioidCDSREC10-artifact-bundle.json");
		loadTransaction(packagePrefix + "OpioidCDSREC10-patient-data-bundle.json");

		Parameters params = org.opencds.cqf.ruler.utility.r4.Parameters.newParameters(
			org.opencds.cqf.ruler.utility.r4.Parameters.newPart(
					"subject", new StringType("Patient/example-rec-10-no-screenings"))
		);
		Parameters result = getClient().operation()
			.onInstance(new IdType("Library", "OpioidCDSREC10PatientView"))
			.named("$evaluate")
			.withParameters(params)
			.returnResourceType(Parameters.class)
			.execute();

		assertNotNull(result);
		assertEquals(10, result.getParameter().size());
		assertTrue(result.hasParameter("Patient"));
		assertTrue(result.getParameter().get(0).hasResource());
		assertTrue(result.getParameter().get(0).getResource() instanceof Patient);
		assertTrue(result.hasParameter(
				"Chronic Pain Opioid Analgesic with Ambulatory Misuse Potential Prescriptions"));
		assertTrue(result.getParameter().get(1).hasResource());
		assertTrue(result.getParameter().get(1).getResource() instanceof MedicationRequest);
		assertTrue(result.hasParameter(
				"Patient Is Being Prescribed Opioid Analgesic with Ambulatory Misuse Potential"));
		assertTrue(((BooleanType) result.getParameter(
				"Patient Is Being Prescribed Opioid Analgesic with Ambulatory Misuse Potential")).booleanValue());
		assertTrue(result.hasParameter("Is Perform Drug Screen Recommendation Applicable?"));
		assertTrue(((BooleanType) result.getParameter(
				"Is Perform Drug Screen Recommendation Applicable?")).booleanValue());
		assertTrue(result.hasParameter("Applicable Because of Positive Cocaine or PCP or Opiates"));
		assertFalse(((BooleanType) result.getParameter(
				"Applicable Because of Positive Cocaine or PCP or Opiates")).booleanValue());
		assertTrue(result.hasParameter("Urine Drug Screening ProcedureRequest Category"));
		assertTrue(result.getParameter(
				"Urine Drug Screening ProcedureRequest Category") instanceof CodeableConcept);
		assertTrue(result.hasParameter("Detail"));
		assertEquals("Patients on opioid therapy should have a urine drug test performed every 12 months.",
				((StringType) result.getParameter("Detail")).getValue());
		assertTrue(result.hasParameter("Indicator"));
		assertEquals("warning", ((StringType) result.getParameter("Indicator")).getValue());
		assertTrue(result.hasParameter("Summary"));
		assertEquals("Annual Urine Screening Check",
				((StringType) result.getParameter("Summary")).getValue());
		assertTrue(result.hasParameter("Urine Drug Screening Request"));
		assertTrue(result.getParameter().get(9).hasResource());
		assertTrue(result.getParameter().get(9).getResource() instanceof ServiceRequest);
	}
}
