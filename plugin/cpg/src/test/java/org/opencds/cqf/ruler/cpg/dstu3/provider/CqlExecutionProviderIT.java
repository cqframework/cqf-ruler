package org.opencds.cqf.ruler.cpg.dstu3.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cpg.CpgConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CqlExecutionProviderIT.class,
		CpgConfig.class }, properties = { "hapi.fhir.fhir_version=dstu3" })
public class CqlExecutionProviderIT extends RestIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(CqlExecutionProviderIT.class);

	@Test
	public void testSimpleArithmeticCqlExecutionProvider() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("expression").setValue(new StringType("5 * 5"));
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getValue() instanceof StringType);
		assertTrue(((StringType) results.getParameter().get(0).getValue()).asStringValue().equals("25"));
		assertTrue(results.getParameter().get(1).getValue() instanceof StringType);
		assertTrue(((StringType) results.getParameter().get(1).getValue()).asStringValue().equals("Integer"));
		logger.debug("Results: ", results);
	}

	@Test
	public void testSimpleRetrieveCqlExecutionProvider() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("subject").setValue(new StringType("Patient/SimplePatient"));
		params.addParameter().setName("expression").setValue(new StringType("[Observation] O where O.status = 'final'"));
		String packagePrefix = "org/opencds/cqf/ruler/cpg/r4/provider/";
		loadResource(packagePrefix + "SimpleObservation.json");
		loadResource(packagePrefix + "SimplePatient.json");
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		logger.debug("Results: ", results);
	}

	@Test
	public void testReferencedLibraryCqlExecutionProvider() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("subject").setValue(new StringType("Patient/SimplePatient"));
		Parameters libraryParameter = new Parameters();
		libraryParameter.addParameter().setName("url")
				.setValue(new StringType(this.getClient().getServerBase() + "Library/SimpleDstu3Library"));
		libraryParameter.addParameter().setName("name").setValue(new StringType("SimpleDstu3Library"));
		params.addParameter().setName("library").setResource(libraryParameter);
		params.addParameter().setName("expression")
				.setValue(new StringType("SimpleDstu3Library.\"simpleBooleanExpression\""));
		String packagePrefix = "org/opencds/cqf/ruler/cpg/dstu3/provider/";
		loadResource(packagePrefix + "SimpleDstu3Library.json");
		loadResource(packagePrefix + "SimplePatient.json");
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getValue() instanceof StringType);
		assertTrue(((StringType) results.getParameter().get(0).getValue()).asStringValue().equals("true"));
		assertTrue(results.getParameter().get(1).getValue() instanceof StringType);
		assertTrue(((StringType) results.getParameter().get(1).getValue()).asStringValue().equals("Boolean"));
		logger.debug("Results: ", results);
	}

	@Test
	public void testDataBundleCqlExecutionProvider() throws Exception {
		Parameters params = new Parameters();
		Parameters libraryParameter = new Parameters();
		libraryParameter.addParameter().setName("url")
				.setValue(new StringType(this.getClient().getServerBase() + "Library/SimpleDstu3Library"));
		libraryParameter.addParameter().setName("name").setValue(new StringType("SimpleDstu3Library"));
		params.addParameter().setName("library").setResource(libraryParameter);
		params.addParameter().setName("expression")
				.setValue(new StringType("SimpleDstu3Library.\"observationRetrieve\""));
		String packagePrefix = "org/opencds/cqf/ruler/cpg/dstu3/provider/";
		loadResource(packagePrefix + "SimpleDstu3Library.json");
		loadResource(packagePrefix + "SimplePatient.json");
		Bundle data = (Bundle) loadResource(packagePrefix + "SimpleDataBundle.json");
		params.addParameter().setName("data").setResource(data);
		params.addParameter().setName("useServerData").setValue(new BooleanType(false));
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getResource() instanceof Bundle);
		assertTrue(((Bundle) results.getParameter().get(0).getResource()).getEntry().get(0)
				.getResource() instanceof Observation);
		assertTrue(results.getParameter().get(1).getValue() instanceof StringType);
		assertTrue(((StringType) results.getParameter().get(1).getValue()).asStringValue().equals("List"));
		logger.debug("Results: ", results);
	}

	@Test
	public void testDataBundleCqlExecutionProviderWithSubject() throws Exception {
		Parameters params = new Parameters();
		Parameters libraryParameter = new Parameters();
		params.addParameter().setName("subject").setValue(new StringType("Patient/SimplePatient"));
		libraryParameter.addParameter().setName("url")
				.setValue(new StringType(this.getClient().getServerBase() + "Library/SimpleDstu3Library"));
		libraryParameter.addParameter().setName("name").setValue(new StringType("SimpleDstu3Library"));
		params.addParameter().setName("library").setResource(libraryParameter);
		params.addParameter().setName("expression")
				.setValue(new StringType("SimpleDstu3Library.\"observationRetrieve\""));
		String packagePrefix = "org/opencds/cqf/ruler/cpg/dstu3/provider/";
		loadResource(packagePrefix + "SimpleDstu3Library.json");
		loadResource(packagePrefix + "SimplePatient.json");
		Bundle data = (Bundle) loadResource(packagePrefix + "SimpleDataBundle.json");
		params.addParameter().setName("data").setResource(data);
		params.addParameter().setName("useServerData").setValue(new BooleanType(false));
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getResource() instanceof Bundle);
		assertTrue(((Bundle) results.getParameter().get(0).getResource()).getEntry().get(0)
				.getResource() instanceof Observation);
		assertTrue(results.getParameter().get(1).getValue() instanceof StringType);
		assertTrue(((StringType) results.getParameter().get(1).getValue()).asStringValue().equals("List"));
		logger.debug("Results: ", results);
	}

	@Test
	public void testSimpleParametersCqlExecutionProvider() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("expression").setValue(new StringType("year from %inputDate before 2020"));
		Parameters evaluationParams = new Parameters();
		evaluationParams.addParameter().setName("%inputDate").setValue(new DateType("2019-11-01"));
		params.addParameter().setName("parameters").setResource(evaluationParams);
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getValue() instanceof StringType);
		assertTrue(((StringType) results.getParameter().get(0).getValue()).asStringValue().equals("true"));
		assertTrue(results.getParameter().get(1).getValue() instanceof StringType);
		assertTrue(((StringType) results.getParameter().get(1).getValue()).asStringValue().equals("Boolean"));
		logger.debug("Results: ", results);
	}

	@Test
	public void testCqlExecutionProviderWithContent() throws Exception {
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

		String packagePrefix = "org/opencds/cqf/ruler/cpg/r4/provider/";
		loadResource(packagePrefix + "SimpleObservation.json");
		loadResource(packagePrefix + "SimplePatient.json");

		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();

		assertFalse(results.isEmpty());
		assertEquals(7, results.getParameter().size());
		assertTrue(results.getParameter().get(1).hasName());
		assertEquals("simpleBooleanExpression", results.getParameter().get(1).getName());
		assertTrue(results.getParameter().get(1).getResource() instanceof Parameters);
		Parameters innerResult = (Parameters) results.getParameter().get(1).getResource();
		assertFalse(innerResult.isEmpty());
		assertTrue(innerResult.getParameter().get(0).hasValue());
		assertEquals("true", innerResult.getParameter().get(0).getValue().primitiveValue());
	}

	@Test
	public void testCqlExecutionProviderWithContentAndExpression() throws Exception {
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

		String packagePrefix = "org/opencds/cqf/ruler/cpg/r4/provider/";
		loadResource(packagePrefix + "SimpleObservation.json");
		loadResource(packagePrefix + "SimplePatient.json");

		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();

		assertFalse(results.isEmpty());
		assertEquals(2, results.getParameter().size());
		assertTrue(results.getParameter().get(0).hasName());
		assertTrue(results.getParameter().get(0).hasValue());
		assertEquals("true", results.getParameter().get(0).getValue().primitiveValue());
	}
}
