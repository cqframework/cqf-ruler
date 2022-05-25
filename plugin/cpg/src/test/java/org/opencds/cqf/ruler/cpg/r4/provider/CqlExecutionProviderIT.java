package org.opencds.cqf.ruler.cpg.r4.provider;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DateType;
// import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cpg.CpgConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CqlExecutionProviderIT.class,
		CpgConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
public class CqlExecutionProviderIT extends RestIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(CqlExecutionProviderIT.class);

	@Test
	public void testSimpleArithmeticCqlExecutionProvider() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("expression").setValue(new StringType("5 * 5"));
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		assertTrue(results.getParameter("value") instanceof StringType);
		assertTrue(((StringType) results.getParameter("value")).asStringValue().equals("25"));
		assertTrue(results.getParameter("resultType") instanceof StringType);
		assertTrue(((StringType) results.getParameter("resultType")).asStringValue().equals("Integer"));
		logger.debug("Results: ", results);
	}

	@Test
	public void testSimpleRetrieveCqlExecutionProvider() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("subject").setValue(new StringType("Patient/SimplePatient"));
		params.addParameter().setName("expression")
				.setValue(new StringType("[Observation] O where O.status = 'final'"));
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
				.setValue(new CanonicalType(this.getClient().getServerBase() + "Library/SimpleR4Library"));
		libraryParameter.addParameter().setName("name").setValue(new StringType("SimpleR4Library"));
		params.addParameter().setName("library").setResource(libraryParameter);
		params.addParameter().setName("expression")
				.setValue(new StringType("SimpleR4Library.\"simpleBooleanExpression\""));
		String packagePrefix = "org/opencds/cqf/ruler/cpg/r4/provider/";
		loadResource(packagePrefix + "SimpleR4Library.json");
		loadResource(packagePrefix + "SimplePatient.json");
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		assertTrue(results.getParameter("value") instanceof StringType);
		assertTrue(((StringType) results.getParameter("value")).asStringValue().equals("true"));
		assertTrue(results.getParameter("resultType") instanceof StringType);
		assertTrue(((StringType) results.getParameter("resultType")).asStringValue().equals("Boolean"));
		logger.debug("Results: ", results);
	}

	@Test
	public void testDataBundleCqlExecutionProvider() throws Exception {
		Parameters params = new Parameters();
		Parameters libraryParameter = new Parameters();
		libraryParameter.addParameter().setName("url")
				.setValue(new CanonicalType(this.getClient().getServerBase() + "Library/SimpleR4Library"));
		libraryParameter.addParameter().setName("name").setValue(new StringType("SimpleR4Library"));
		params.addParameter().setName("library").setResource(libraryParameter);
		params.addParameter().setName("expression").setValue(new StringType("SimpleR4Library.\"observationRetrieve\""));
		String packagePrefix = "org/opencds/cqf/ruler/cpg/r4/provider/";
		loadResource(packagePrefix + "SimpleR4Library.json");
		loadResource(packagePrefix + "SimplePatient.json");
		Bundle data = (Bundle) loadResource(packagePrefix + "SimpleDataBundle.json");
		params.addParameter().setName("data").setResource(data);
		params.addParameter().setName("useServerData").setValue(new BooleanType(false));
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getResource() instanceof Bundle);
		assertTrue(((Bundle) results.getParameter().get(0).getResource()).getEntry().get(0)
				.getResource() instanceof Observation);
		assertTrue(results.getParameter("resultType") instanceof StringType);
		assertTrue(((StringType) results.getParameter("resultType")).asStringValue().equals("List"));
		logger.debug("Results: ", results);
	}

	@Test
	public void testDataBundleCqlExecutionProviderWithSubject() throws Exception {
		Parameters params = new Parameters();
		Parameters libraryParameter = new Parameters();
		params.addParameter().setName("subject").setValue(new StringType("Patient/SimplePatient"));
		libraryParameter.addParameter().setName("url")
				.setValue(new CanonicalType(this.getClient().getServerBase() + "Library/SimpleR4Library"));
		libraryParameter.addParameter().setName("name").setValue(new StringType("SimpleR4Library"));
		params.addParameter().setName("library").setResource(libraryParameter);
		params.addParameter().setName("expression").setValue(new StringType("SimpleR4Library.\"observationRetrieve\""));
		String packagePrefix = "org/opencds/cqf/ruler/cpg/r4/provider/";
		loadResource(packagePrefix + "SimpleR4Library.json");
		loadResource(packagePrefix + "SimplePatient.json");
		Bundle data = (Bundle) loadResource(packagePrefix + "SimpleDataBundle.json");
		params.addParameter().setName("data").setResource(data);
		params.addParameter().setName("useServerData").setValue(new BooleanType(false));
		Parameters results = getClient().operation().onServer().named("$cql").withParameters(params).execute();
		assertTrue(results.getParameter().get(0).getResource() instanceof Bundle);
		assertTrue(((Bundle) results.getParameter().get(0).getResource()).getEntry().get(0)
				.getResource() instanceof Observation);
		assertTrue(results.getParameter("resultType") instanceof StringType);
		assertTrue(((StringType) results.getParameter("resultType")).asStringValue().equals("List"));
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
		assertTrue(results.getParameter("value") instanceof StringType);
		assertTrue(((StringType) results.getParameter("value")).asStringValue().equals("true"));
		assertTrue(results.getParameter("resultType") instanceof StringType);
		assertTrue(((StringType) results.getParameter("resultType")).asStringValue().equals("Boolean"));
		logger.debug("Results: ", results);
	}
}
