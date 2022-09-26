package org.opencds.cqf.ruler.cpg.r4.provider;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cpg.CpgConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.ruler.utility.r4.Parameters.booleanPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.canonicalPart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.datePart;
import static org.opencds.cqf.ruler.utility.r4.Parameters.parameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.part;
import static org.opencds.cqf.ruler.utility.r4.Parameters.stringPart;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = { CqlExecutionProviderIT.class, CpgConfig.class },
		properties = { "hapi.fhir.fhir_version=r4" })
class CqlExecutionProviderIT extends RestIntegrationTest {
	private final String packagePrefix = "org/opencds/cqf/ruler/cpg/r4/provider/";

	@BeforeEach
	void setup() {
		loadResource(packagePrefix + "SimpleR4Library.json");
		loadResource(packagePrefix + "SimplePatient.json");
		loadResource(packagePrefix + "SimpleObservation.json");
		loadResource(packagePrefix + "SimpleCondition.json");
	}

	@Test
	void testSimpleArithmeticCqlExecutionProvider() {
		Parameters params = parameters(stringPart("expression", "5 * 5"));
		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();
		assertTrue(results.getParameter("return") instanceof IntegerType);
		assertEquals("25", ((IntegerType) results.getParameter("return")).asStringValue());
	}

	@Test
	void testSimpleRetrieveCqlExecutionProvider() {
		Parameters params = parameters(
				stringPart("subject", "SimplePatient",
						stringPart("expression", "[Observation]")));
		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();
		// TODO: result is always null...
		// assertTrue(results.hasParameter());
	}

	@Test
	void testReferencedLibraryCqlExecutionProvider() {
		Parameters libraryParameter = parameters(
				canonicalPart("url", this.getClient().getServerBase() + "Library/SimpleR4Library"),
				stringPart("name", "SimpleR4Library"));
		Parameters params = parameters(
				stringPart("subject", "SimplePatient"),
				part("library", libraryParameter),
				stringPart("expression", "SimpleR4Library.\"simpleBooleanExpression\""));
		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();
		assertTrue(results.getParameter("return") instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter("return")).booleanValue());
	}

	@Test
	void testDataBundleCqlExecutionProvider() {
		Parameters libraryParameter = parameters(
				canonicalPart("url", this.getClient().getServerBase() + "Library/SimpleR4Library"),
				stringPart("name", "SimpleR4Library"));
		Bundle data = (Bundle) loadResource(packagePrefix + "SimpleDataBundle.json");
		Parameters params = parameters(
				part("library", libraryParameter),
				stringPart("expression","SimpleR4Library.\"observationRetrieve\""),
				part("data", data),
				booleanPart("useServerData", false));
		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getResource() instanceof Observation);
	}

	@Test
	void testDataBundleCqlExecutionProviderWithSubject() {
		Parameters libraryParameter = parameters(
				canonicalPart("url", this.getClient().getServerBase() + "Library/SimpleR4Library"),
				stringPart("name", "SimpleR4Library"));
		Bundle data = (Bundle) loadResource(packagePrefix + "SimpleDataBundle.json");
		Parameters params = parameters(
				stringPart("subject", "SimplePatient"),
				part("library", libraryParameter),
				stringPart("expression", "SimpleR4Library.\"observationRetrieve\""),
				part("data", data),
				booleanPart("useServerData", false));
		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getResource() instanceof Observation);
	}

	@Test
	void testSimpleParametersCqlExecutionProvider() {
		Parameters evaluationParams = parameters(
				datePart("%inputDate", "2019-11-01"));
		Parameters params = parameters(
				stringPart("expression", "year from %inputDate before 2020"),
				part("parameters", evaluationParams));
		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();
		assertTrue(results.getParameter("return") instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter("return")).booleanValue());
	}

	@Test
	void testCqlExecutionProviderWithContent() {
		Parameters params = parameters(
				stringPart("subject", "SimplePatient"),
				stringPart("content", "library SimpleR4Library\n" +
								"\n" +
								"using FHIR version '4.0.1'\n" +
								"\n" +
								"include FHIRHelpers version '4.0.1' called FHIRHelpers\n" +
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
								"define \"Numerator\": \"Denominator\""));

		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();

		assertFalse(results.isEmpty());
		assertEquals(7, results.getParameter().size());
		assertTrue(results.hasParameter("Patient"));
		assertTrue(results.getParameter().get(0).hasResource());
		assertTrue(results.getParameter().get(0).getResource() instanceof Patient);
		assertTrue(results.hasParameter("simpleBooleanExpression"));
		assertTrue(results.getParameter("simpleBooleanExpression") instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter("simpleBooleanExpression")).booleanValue());
		assertTrue(results.hasParameter("observationRetrieve"));
		assertTrue(results.getParameter().get(2).hasResource());
		assertTrue(results.getParameter().get(2).getResource() instanceof Observation);
		assertTrue(results.hasParameter("observationHasCode"));
		assertTrue(results.getParameter("observationHasCode") instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter("observationHasCode")).booleanValue());
		assertTrue(results.hasParameter("Initial Population"));
		assertTrue(results.getParameter("Initial Population") instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter("Initial Population")).booleanValue());
		assertTrue(results.hasParameter("Numerator"));
		assertTrue(results.getParameter("Numerator") instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter("Numerator")).booleanValue());
		assertTrue(results.hasParameter("Denominator"));
		assertTrue(results.getParameter("Denominator") instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter("Denominator")).booleanValue());
	}

	@Test
	void testCqlExecutionProviderWithContentAndExpression() {
		Parameters params = parameters(
				stringPart("subject", "SimplePatient"),
				stringPart("expression", "Numerator"),
				stringPart("content","library SimpleR4Library\n" +
								"\n" +
								"using FHIR version '4.0.1'\n" +
								"\n" +
								"include FHIRHelpers version '4.0.1' called FHIRHelpers\n" +
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
								"define \"Numerator\": \"Denominator\""));

		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();

		assertFalse(results.isEmpty());
		assertEquals(1, results.getParameter().size());
		assertTrue(results.getParameter("Numerator") instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter("Numerator")).booleanValue());
	}

	@Test
	void testContentRetrieveWithInlineCode() {
		Parameters params = parameters(
				stringPart("subject", "SimplePatient"),
				stringPart("content", "library AsthmaTest version '1.0.0'\n" +
								"\n" +
								"using FHIR version '4.0.1'\n" +
								"\n" +
								"include FHIRHelpers version '4.0.1'\n" +
								"\n" +
								"codesystem \"SNOMED\": 'http://snomed.info/sct'\n" +
								"\n" +
								"code \"Asthma\": '195967001' from \"SNOMED\"\n" +
								"\n" +
								"context Patient\n" +
								"\n" +
								"define \"Asthma Diagnosis\":\n" +
								"    [Condition: \"Asthma\"]\n" +
								"\n" +
								"define \"Has Asthma Diagnosis\":\n" +
								"    exists(\"Asthma Diagnosis\")\n"));

		Parameters results = getClient().operation().onServer().named("$cql")
				.withParameters(params).execute();

		assertTrue(results.hasParameter());
		assertEquals(3, results.getParameter().size());
		assertTrue(results.getParameterFirstRep().hasResource());
		assertTrue(results.getParameterFirstRep().getResource() instanceof Patient);
		assertTrue(results.getParameter().get(1).hasResource());
		assertTrue(results.getParameter().get(1).getResource() instanceof Condition);
		assertTrue(results.getParameter().get(2).hasValue());
		assertTrue(((BooleanType) results.getParameter().get(2).getValue()).booleanValue());
	}

	@Test
	void testErrorExpression() {
		Parameters params = parameters(stringPart("expression", "Interval[1,5]"));
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
