package org.opencds.cqf.ruler.cr.dstu3.provider;

import static org.opencds.cqf.ruler.utility.dstu3.Parameters.parameters;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.part;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.stringPart;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Parameters;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = { MeasureEvaluateProviderIT.class, CrConfig.class, CqlConfig.class },
		properties = { "hapi.fhir.fhir_version=dstu3" })
class MeasureEvaluateProviderIT extends RestIntegrationTest {

	@Test
	void testMeasureEvaluate() {
		loadTransaction("Exm105Fhir3Measure.json");

		Parameters params = parameters(
				stringPart("periodStart", "2019-01-01"),
				stringPart("periodEnd", "2020-01-01"),
				stringPart("reportType", "individual"),
				stringPart("subject", "Patient/denom-EXM105-FHIR3"),
				stringPart("lastReceivedOn", "2019-12-12")
		);

		MeasureReport returnMeasureReport = getClient().operation()
				.onInstance(new IdType("Measure", "measure-EXM105-FHIR3-8.0.000"))
				.named("$evaluate-measure")
				.withParameters(params)
				.returnResourceType(MeasureReport.class)
				.execute();

		assertNotNull(returnMeasureReport);
	}

	@Test
	void testMeasureEvaluateWithTerminology() {
		loadTransaction("Exm105Fhir3Measure.json");

		Endpoint terminologyEndpoint = (Endpoint) loadResource("Endpoint.json");
		terminologyEndpoint.setAddress(String.format("http://localhost:%s/fhir/", getPort()));

		Parameters params = parameters(
				stringPart("periodStart", "2019-01-01"),
				stringPart("periodEnd", "2020-01-01"),
				stringPart("reportType", "individual"),
				stringPart("subject", "Patient/denom-EXM105-FHIR3"),
				stringPart("lastReceivedOn", "2019-12-12"),
				part("terminologyEndpoint", terminologyEndpoint)
		);

		MeasureReport returnMeasureReport = getClient().operation()
				.onInstance(new IdType("Measure", "measure-EXM105-FHIR3-8.0.000"))
				.named("$evaluate-measure")
				.withParameters(params)
				.returnResourceType(MeasureReport.class)
				.execute();

		assertNotNull(returnMeasureReport);
	}

	@Test
	void testMeasureEvaluateWithAdditionalData() {
		loadTransaction("Exm105FhirR3MeasurePartBundle.json");
		Bundle additionalData = (Bundle) loadResource("Exm105FhirR3MeasureAdditionalData.json");

		Parameters params = parameters(
				stringPart("periodStart", "2019-01-01"),
				stringPart("periodEnd", "2020-01-01"),
				stringPart("reportType", "individual"),
				stringPart("subject", "Patient/denom-EXM105-FHIR3"),
				stringPart("lastReceivedOn", "2019-12-12"),
				part("additionalData", additionalData)
		);

		MeasureReport returnMeasureReport = getClient().operation()
				.onInstance(new IdType("Measure", "measure-EXM105-FHIR3-8.0.000"))
				.named("$evaluate-measure")
				.withParameters(params)
				.returnResourceType(MeasureReport.class)
				.execute();

		assertNotNull(returnMeasureReport);
	}
}
