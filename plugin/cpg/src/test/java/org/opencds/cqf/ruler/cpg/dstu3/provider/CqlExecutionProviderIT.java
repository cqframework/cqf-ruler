package org.opencds.cqf.ruler.cpg.dstu3.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cpg.CpgConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CqlExecutionProviderIT.class, CpgConfig.class }, properties = { "hapi.fhir.fhir_version=dstu3" })
class CqlExecutionProviderIT extends RestIntegrationTest {

	private String packagePrefix = "org/opencds/cqf/ruler/cpg/dstu3/provider/";

	@BeforeEach
	void setup() {
		loadResource(packagePrefix + "SimpleDstu3Library.json");
		loadResource(packagePrefix + "SimpleObservation.json");
		loadResource(packagePrefix + "SimplePatient.json");
	}

	@Test
	void testSimpleArithmeticCqlExecutionProvider() {
		Parameters params = new Parameters();
		params.addParameter().setName("expression").setValue(new StringType("5 * 5"));
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getValue() instanceof IntegerType);
		assertEquals("25", ((IntegerType) results.getParameter().get(0).getValue()).asStringValue());
	}

	@Test
	void testSimpleRetrieveCqlExecutionProvider() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("subject").setValue(new StringType("SimplePatient"));
		params.addParameter().setName("expression").setValue(new StringType("[Observation]"));
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		// TODO: result is always null...
		// assertTrue(results.hasParameter());
	}

	@Test
	void testReferencedLibraryCqlExecutionProvider() {
		Parameters params = new Parameters();
		params.addParameter().setName("subject").setValue(new StringType("SimplePatient"));
		Parameters libraryParameter = new Parameters();
		libraryParameter.addParameter().setName("url")
				.setValue(new StringType(this.getClient().getServerBase() + "Library/SimpleDstu3Library"));
		libraryParameter.addParameter().setName("name").setValue(new StringType("SimpleDstu3Library"));
		params.addParameter().setName("library").setResource(libraryParameter);
		params.addParameter().setName("expression")
				.setValue(new StringType("SimpleDstu3Library.\"simpleBooleanExpression\""));
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getValue() instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter().get(0).getValue()).booleanValue());
	}

	@Test
	void testDataBundleCqlExecutionProvider() {
		Parameters params = new Parameters();
		Parameters libraryParameter = new Parameters();
		libraryParameter.addParameter().setName("url")
				.setValue(new StringType(this.getClient().getServerBase() + "Library/SimpleDstu3Library"));
		libraryParameter.addParameter().setName("name").setValue(new StringType("SimpleDstu3Library"));
		params.addParameter().setName("library").setResource(libraryParameter);
		params.addParameter().setName("expression")
				.setValue(new StringType("SimpleDstu3Library.\"observationRetrieve\""));
		Bundle data = (Bundle) loadResource(packagePrefix + "SimpleDataBundle.json");
		params.addParameter().setName("data").setResource(data);
		params.addParameter().setName("useServerData").setValue(new BooleanType(false));
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getResource() instanceof Observation);
	}

	@Test
	void testDataBundleCqlExecutionProviderWithSubject() {
		Parameters params = new Parameters();
		Parameters libraryParameter = new Parameters();
		params.addParameter().setName("subject").setValue(new StringType("SimplePatient"));
		libraryParameter.addParameter().setName("url")
				.setValue(new StringType(this.getClient().getServerBase() + "Library/SimpleDstu3Library"));
		libraryParameter.addParameter().setName("name").setValue(new StringType("SimpleDstu3Library"));
		params.addParameter().setName("library").setResource(libraryParameter);
		params.addParameter().setName("expression")
				.setValue(new StringType("SimpleDstu3Library.\"observationRetrieve\""));
		Bundle data = (Bundle) loadResource(packagePrefix + "SimpleDataBundle.json");
		params.addParameter().setName("data").setResource(data);
		params.addParameter().setName("useServerData").setValue(new BooleanType(false));
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getResource() instanceof Observation);
	}

	@Test
	void testSimpleParametersCqlExecutionProvider() {
		Parameters params = new Parameters();
		params.addParameter().setName("expression").setValue(new StringType("year from %inputDate before 2020"));
		Parameters evaluationParams = new Parameters();
		evaluationParams.addParameter().setName("%inputDate").setValue(new DateType("2019-11-01"));
		params.addParameter().setName("parameters").setResource(evaluationParams);
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getValue() instanceof BooleanType);
		assertTrue(((BooleanType) results.getParameter().get(0).getValue()).booleanValue());
	}

	@Test
	void testCqlExecutionProviderWithContent() {
		Parameters params = new Parameters();
		params.addParameter().setName("subject").setValue(new StringType("Patient/SimplePatient"));
		params.addParameter()
				.setName("content")
				.setValue(
						new StringType(
								"library SimpleR4Library\n" +
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
										"define \"Numerator\": \"Denominator\""));

		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();

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
		Parameters params = new Parameters();
		params.addParameter().setName("subject").setValue(new StringType("Patient/SimplePatient"));
		params.addParameter().setName("expression").setValue(new StringType("Numerator"));
		params.addParameter()
				.setName("content")
				.setValue(
						new StringType(
								"library SimpleR4Library\n" +
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
										"define \"Numerator\": \"Denominator\""));

		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();

		assertFalse(results.isEmpty());
		assertEquals(1, results.getParameter().size());
		assertTrue(results.getParameter().get(0).hasName());
		assertTrue(results.getParameter().get(0).hasValue());
		assertTrue(((BooleanType) results.getParameter().get(0).getValue()).booleanValue());
	}
}
