package org.opencds.cqf.ruler.cr.r4.provider;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CareGapsProviderIT.class,
		CrConfig.class }, properties = {
				"hapi.fhir.fhir_version=r4",
		})
public class CareGapsProviderIT extends RestIntegrationTest {

	private static final String periodStartValid = "2022-01-01";
	private static final String periodEndValid = "2022-01-15";
	private static final String subjectValid = "Patient/12345";
	private static final String statusValid = "open-gap";

	@Test
	public void testPeriodStartValid() throws Exception {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType(periodStartValid));
		params.addParameter().setName("periodEnd").setValue(new StringType(periodEndValid));
		params.addParameter().setName("subject").setValue(new StringType(subjectValid));
		params.addParameter().setName("status").setValue(new StringType(statusValid));

		assertDoesNotThrow(() -> {
			getClient().operation().onType(Measure.class).named("$care-gaps")
					.withParameters(params)
					.useHttpGet()
					.returnResourceType(Parameters.class)
					.execute();
		});
	}
}
