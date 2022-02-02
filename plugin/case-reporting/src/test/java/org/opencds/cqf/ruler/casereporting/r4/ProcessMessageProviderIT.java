package org.opencds.cqf.ruler.casereporting.r4;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.casereporting.CaseReportingConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { ProcessMessageProviderIT.class,
		CaseReportingConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
public class ProcessMessageProviderIT extends RestIntegrationTest {

	@Test
	public void testProcessMessage() throws IOException {

		String packagePrefix = "org/opencds/cqf/ruler/casereporting/r4/";

		Bundle bundle = (Bundle) loadResource(packagePrefix + "example-eicr.json");

		Bundle returnBundle = getClient().operation().onServer()
				.named("$process-message-bundle")
				.withParameter(Parameters.class, "content", bundle)
				.returnResourceType(Bundle.class)
				.execute();

		assertNotNull(returnBundle);
		assertNotNull(
				getClient().read().resource(Patient.class).withId("patient-12742542").execute());
		assertNotNull(
				getClient().read().resource(Encounter.class).withId("encounter-97953898").execute());
		assertNotNull(
				getClient().read().resource(MeasureReport.class).withId("diabetes-mp").execute());
	}

}
