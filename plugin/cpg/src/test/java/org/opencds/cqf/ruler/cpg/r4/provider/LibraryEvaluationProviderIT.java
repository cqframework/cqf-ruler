package org.opencds.cqf.ruler.cpg.r4.provider;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cpg.CpgConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { LibraryEvaluationProviderIT.class, CpgConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
class LibraryEvaluationProviderIT extends RestIntegrationTest {

	private final String packagePrefix = "org/opencds/cqf/ruler/cpg/r4/provider/";

	@Test
	void testSimpleAsthmaInlineCode() {
		loadResource(packagePrefix + "SimplePatient.json");
		loadResource(packagePrefix + "SimpleCondition.json");
		loadResource(packagePrefix + "AsthmaTest.json");
		Parameters params = org.opencds.cqf.ruler.utility.r4.Parameters.newParameters(
			org.opencds.cqf.ruler.utility.r4.Parameters.newPart("subject", new StringType("Patient/SimplePatient"))
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
			org.opencds.cqf.ruler.utility.r4.Parameters.newPart("subject", new StringType("Patient/SimplePatient"))
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
}
