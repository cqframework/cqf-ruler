package org.opencds.cqf.ruler.cpg.dstu3.provider;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cpg.CpgConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newPart;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = { CqlExecutionProviderIT.class, CpgConfig.class },
		properties = { "hapi.fhir.fhir_version=dstu3" })
class CqlExecutionProviderIT extends RestIntegrationTest {

	private final String packagePrefix = "org/opencds/cqf/ruler/cpg/dstu3/provider/";

	@BeforeEach
	void setup() {
		loadResource(packagePrefix + "SimpleDstu3Library.json");
		loadResource(packagePrefix + "SimpleObservation.json");
		loadResource(packagePrefix + "SimplePatient.json");
	}

	@Test
	void testSimpleArithmeticCqlExecutionProvider() {
		Parameters params = newParameters(newPart("expression", new StringType("5 * 5")));
		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getValue() instanceof IntegerType);
		assertEquals("25", ((IntegerType) results.getParameter().get(0).getValue()).asStringValue());
	}

	@Test
	void testSimpleRetrieveCqlExecutionProvider() {
		Parameters params = newParameters(
				newPart("subject", new StringType("SimplePatient")),
				newPart("expression", new StringType("[Observation]")));
		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();
		// TODO: result is always null...
		// assertTrue(results.hasParameter());
	}

	@Test
	void testReferencedLibraryCqlExecutionProvider() {
		Parameters libraryParameter = newParameters(
				newPart("url", new StringType(
						this.getClient().getServerBase() + "Library/SimpleDstu3Library")),
				newPart("name", new StringType("SimpleDstu3Library")));
		Parameters params = newParameters(
				newPart("subject", new StringType("SimplePatient")),
				newPart("library", libraryParameter),
				newPart("expression", new StringType(
						"SimpleDstu3Library.\"simpleBooleanExpression\"")));
		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getValue() instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter().get(0).getValue()).booleanValue());
	}

	@Test
	void testDataBundleCqlExecutionProvider() {
		Parameters libraryParameter = newParameters(
				newPart("url", new StringType(
						this.getClient().getServerBase() + "Library/SimpleDstu3Library")),
				newPart("name", new StringType("SimpleDstu3Library")));
		Bundle data = (Bundle) loadResource(packagePrefix + "SimpleDataBundle.json");
		Parameters params = newParameters(
				newPart("library", libraryParameter),
				newPart("expression", new StringType("SimpleDstu3Library.\"observationRetrieve\"")),
				newPart("data", data),
				newPart("useServerData", new BooleanType(false)));
		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getResource() instanceof Observation);
	}

	@Test
	void testDataBundleCqlExecutionProviderWithSubject() {
		Parameters libraryParameter = newParameters(
				newPart("url", new StringType(
						this.getClient().getServerBase() + "Library/SimpleDstu3Library")),
				newPart("name", new StringType("SimpleDstu3Library")));
		Bundle data = (Bundle) loadResource(packagePrefix + "SimpleDataBundle.json");
		Parameters params = newParameters(
				newPart("subject", new StringType("SimplePatient")),
				newPart("library", libraryParameter),
				newPart("expression", new StringType("SimpleDstu3Library.\"observationRetrieve\"")),
				newPart("data", data),
				newPart("useServerData", new BooleanType(false)));
		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getResource() instanceof Observation);
	}

	@Test
	void testSimpleParametersCqlExecutionProvider() {
		Parameters evaluationParams = newParameters(
				newPart("%inputDate", new DateType("2019-11-01")));
		Parameters params = newParameters(
				newPart("expression", new StringType("year from %inputDate before 2020")),
				newPart("parameters", evaluationParams));
		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getValue() instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter().get(0).getValue()).booleanValue());
	}

	@Test
	void testCqlExecutionProviderWithContent() {
		Parameters params = newParameters(
				newPart("subject", new StringType("Patient/SimplePatient")),
				newPart("content", new StringType("library SimpleSTU3Library\n" +
								"\n" +
								"using FHIR version '3.0.1'\n" +
								"\n" +
								"include FHIRHelpers version '3.0.1' called FHIRHelpers\n" +
								"\n" +
								"context Patient\n" +
								"\n" +
								"define simpleBooleanExpression: true\n" +
								"\n" +
								"define observationRetrieve: [Observation]\n" +
								"\n" +
								"define observationHasCode: not IsNull(([Observation]).code)\n" +
								"\n" +
								"define \"Initial Population\": observationHasCode\n" +
								"\n" +
								"define \"Denominator\": \"Initial Population\"\n" +
								"\n" +
								"define \"Numerator\": \"Denominator\"")));

		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();

		assertFalse(results.isEmpty());
		assertEquals(7, results.getParameter().size());
		assertTrue(results.getParameter().get(0).hasResource());
		assertTrue(results.getParameter().get(0).getResource() instanceof Patient);
		assertTrue(results.getParameter().get(1).getValue() instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter().get(1).getValue()).booleanValue());
		assertTrue(results.getParameter().get(2).hasResource());
		assertTrue(results.getParameter().get(2).getResource() instanceof Observation);
		assertTrue(results.getParameter().get(3).getValue() instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter().get(3).getValue()).booleanValue());
		assertTrue(results.getParameter().get(4).getValue() instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter().get(4).getValue()).booleanValue());
		assertTrue(results.getParameter().get(5).getValue() instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter().get(5).getValue()).booleanValue());
		assertTrue(results.getParameter().get(6).getValue() instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter().get(6).getValue()).booleanValue());
	}

	@Test
	void testCqlExecutionProviderWithContentAndExpression() {
		Parameters params = newParameters(
				newPart("subject", new StringType("Patient/SimplePatient")),
				newPart("expression", new StringType("Numerator")),
				newPart("content", new StringType("library SimpleSTU3Library\n" +
								"\n" +
								"using FHIR version '3.0.1'\n" +
								"\n" +
								"include FHIRHelpers version '3.0.1' called FHIRHelpers\n" +
								"\n" +
								"context Patient\n" +
								"\n" +
								"define simpleBooleanExpression: true\n" +
								"\n" +
								"define observationRetrieve: [Observation]\n" +
								"\n" +
								"define observationHasCode: not IsNull(([Observation]).code)\n" +
								"\n" +
								"define \"Initial Population\": observationHasCode\n" +
								"\n" +
								"define \"Denominator\": \"Initial Population\"\n" +
								"\n" +
								"define \"Numerator\": \"Denominator\"")));

		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();

		assertFalse(results.isEmpty());
		assertEquals(1, results.getParameter().size());
		assertTrue(results.getParameter().get(0).hasName());
		assertTrue(results.getParameter().get(0).hasValue());
		assertTrue(((BooleanType) results.getParameter().get(0).getValue()).booleanValue());
	}

	@Test
	void testErrorExpression() {
		Parameters params = newParameters(newPart("expression", new StringType("Interval[1,5]")));
		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();
		assertTrue(results.hasParameter());
		assertTrue(results.getParameterFirstRep().hasName());
		assertEquals("evaluation error", results.getParameterFirstRep().getName());
		assertTrue(results.getParameterFirstRep().hasResource());
		assertTrue(results.getParameterFirstRep().getResource() instanceof OperationOutcome);
		assertEquals("Unsupported interval point type for FHIR conversion java.lang.Integer",
				((OperationOutcome) results.getParameterFirstRep().getResource()).getIssueFirstRep().getDetails().getText());
	}
}
