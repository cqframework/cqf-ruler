package org.opencds.cqf.ruler.cpg.dstu3.provider;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cpg.CpgConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { LibraryEvaluationProviderIT.class, CpgConfig.class }, properties = { "hapi.fhir.fhir_version=dstu3" })
class LibraryEvaluationProviderIT extends RestIntegrationTest {

	private final String packagePrefix = "org/opencds/cqf/ruler/cpg/dstu3/provider/";

	@Test
	void testSimpleAsthmaInlineCode() {
		loadResource(packagePrefix + "SimplePatient.json");
		loadResource(packagePrefix + "SimpleCondition.json");
		loadResource(packagePrefix + "AsthmaTest.json");
		Parameters params = org.opencds.cqf.ruler.utility.dstu3.Parameters.newParameters(
			org.opencds.cqf.ruler.utility.dstu3.Parameters.newPart("subject", new StringType("Patient/SimplePatient"))
		);
		Parameters result = getClient().operation()
			.onInstance(new IdType("Library", "AsthmaTest"))
			.named("$evaluate")
			.withParameters(params)
			.returnResourceType(Parameters.class)
			.execute();

		assertNotNull(result);
		assertTrue(result.hasParameter());
		List<Parameters.ParametersParameterComponent> asthmaDiagnosis = result.getParameter().stream().filter(param -> param.getName().equals("Has Asthma Diagnosis")).collect(Collectors.toList());
		assertFalse(asthmaDiagnosis.isEmpty());
		assertTrue(((BooleanType) asthmaDiagnosis.get(0).getValue()).booleanValue());
	}

	@Test
	void testSimpleLibrary() {
		loadResource(packagePrefix + "SimplePatient.json");
		loadResource(packagePrefix + "SimpleObservation.json");
		loadResource(packagePrefix + "SimpleDstu3Library.json");
		Parameters params = org.opencds.cqf.ruler.utility.dstu3.Parameters.newParameters(
			org.opencds.cqf.ruler.utility.dstu3.Parameters.newPart("subject", new StringType("Patient/SimplePatient"))
		);
		Parameters result = getClient().operation()
			.onInstance(new IdType("Library", "SimpleDstu3Library"))
			.named("$evaluate")
			.withParameters(params)
			.returnResourceType(Parameters.class)
			.execute();

		assertNotNull(result);
		assertTrue(result.hasParameter());
		List<Parameters.ParametersParameterComponent> initialPopulation = result.getParameter().stream().filter(param -> param.getName().equals("Initial Population")).collect(Collectors.toList());
		assertFalse(initialPopulation.isEmpty());
		assertTrue(((BooleanType) initialPopulation.get(0).getValue()).booleanValue());
		List<Parameters.ParametersParameterComponent> numerator = result.getParameter().stream().filter(param -> param.getName().equals("Numerator")).collect(Collectors.toList());
		assertFalse(numerator.isEmpty());
		assertTrue(((BooleanType) numerator.get(0).getValue()).booleanValue());
		List<Parameters.ParametersParameterComponent> denominator = result.getParameter().stream().filter(param -> param.getName().equals("Denominator")).collect(Collectors.toList());
		assertFalse(denominator.isEmpty());
		assertTrue(((BooleanType) denominator.get(0).getValue()).booleanValue());
	}
}
