package org.opencds.cqf.ruler.casereporting.r4;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.casereporting.CaseReportingConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
		MeasureDataProcessProviderIT.class, CaseReportingConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
class MeasureDataProcessProviderIT extends RestIntegrationTest {
	@Test
	void testMeasureReportExtractLineListData() {
		loadResource("Patient-ra-patient01.json");
		loadResource("Patient-ra-patient02.json");
		loadResource("Patient-ra-patient03.json");
		loadResource("Group-ra-group00.json");
		loadResource("Group-ra-group01.json");
		loadResource("Group-ra-group02.json");
		loadResource("MeasureReport-ra-measurereport01.json");

		MeasureReport measureReport = getClient().read().resource(MeasureReport.class)
				.withId("ra-measurereport01").execute();

		assertNotNull(measureReport);

		Parameters params = parameters(part("measureReport", measureReport));

		Bundle returnBundle = getClient().operation().onType(MeasureReport.class)
				.named("$extract-line-list-data")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();

		assertNotNull(returnBundle);
	}
}
