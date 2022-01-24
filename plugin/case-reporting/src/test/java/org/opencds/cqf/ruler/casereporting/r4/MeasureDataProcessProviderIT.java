package org.opencds.cqf.ruler.casereporting.r4;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.casereporting.CaseReportingConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
		CaseReportingConfig.class }, properties = { "hapi.fhir.fhir_version=r4",
				"spring.main.allow-bean-definition-overriding=true",
				"debug=true",
				"spring.batch.job.enabled=false" })
public class MeasureDataProcessProviderIT extends RestIntegrationTest {
	@Test
	public void testMeasureReportExtractLineListData() throws IOException {

		String packagePrefix = "org/opencds/cqf/ruler/casereporting/r4/";
		loadResource(packagePrefix + "Patient-ra-patient01.json");
		loadResource(packagePrefix + "Patient-ra-patient02.json");
		loadResource(packagePrefix + "Patient-ra-patient03.json");
		loadResource(packagePrefix + "Group-ra-group00.json");
		loadResource(packagePrefix + "Group-ra-group01.json");
		loadResource(packagePrefix + "Group-ra-group02.json");
		loadResource(packagePrefix + "MeasureReport-ra-measurereport01.json");

		MeasureReport measureReport = getClient().read().resource(MeasureReport.class).withId("ra-measurereport01")
				.execute();

		assertNotNull(measureReport);

		Parameters params = new Parameters();
		params.addParameter().setName("measureReport").setResource(measureReport);
		params.addParameter().setName("subjectList").setValue(null);

		Bundle returnBundle = getClient().operation().onType(MeasureReport.class)
				.named("$extract-line-list-data")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();

		assertNotNull(returnBundle);
	}
}
