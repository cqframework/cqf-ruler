package org.opencds.cqf.ruler.cr.r4.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
		MeasureEvaluateAdditionalDataTest.class, CrConfig.class, CqlConfig.class }, properties = {
				"hapi.fhir.fhir_version=r4"
		})
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MeasureEvaluateAdditionalDataTest extends RestIntegrationTest {

	@Override
	public void baseAfterAll() {
		// Intentionally empty to override the base class behavior of resetting once per
		// test suite
	}

	@AfterEach
	public void afterEach() {
		getDbService().resetDatabase();
	}

	@Test
	public void testMeasureEvaluateWithXmlAdditionalData() throws Exception {
		String mainBundleAsText = stringFromResource("ClientNonPatientBasedMeasureBundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(mainBundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		String parametersAsText = stringFromResource("Parameters.xml");
		Parameters parameters = (Parameters) getFhirContext().newXmlParser().parseResource(parametersAsText);

		loadResource("Patient-hypo.json");

		MeasureReport returnMeasureReport = getClient().operation()
				.onInstance(new IdType("Measure", "InitialInpatientPopulation"))
				.named("$evaluate-measure")
				.withParameters(parameters)
				.returnResourceType(MeasureReport.class)
				.execute();

		assertNotNull(returnMeasureReport);
	}

	@Test
	public void testMeasureEvaluateWithAdditionalData() throws Exception {

		String mainBundleAsText = stringFromResource("Exm104FhirR4MeasurePartBundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(mainBundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		String additionalBundleAsText = stringFromResource("Exm104FhirR4MeasureAdditionalData.json");
		Bundle additionalData = (Bundle) getFhirContext().newJsonParser().parseResource(additionalBundleAsText);

		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType("2019-01-01"));
		params.addParameter().setName("periodEnd").setValue(new StringType("2020-01-01"));
		params.addParameter().setName("reportType").setValue(new StringType("subject"));
		params.addParameter().setName("subject").setValue(new StringType("Patient/numer-EXM104"));
		params.addParameter().setName("lastReceivedOn").setValue(new StringType("2019-12-12"));
		params.addParameter().setName("additionalData").setResource(additionalData);

		MeasureReport returnMeasureReport = getClient().operation()
				.onInstance(new IdType("Measure", "measure-EXM104-8.2.000"))
				.named("$evaluate-measure")
				.withParameters(params)
				.returnResourceType(MeasureReport.class)
				.execute();

		assertNotNull(returnMeasureReport);
	}
}