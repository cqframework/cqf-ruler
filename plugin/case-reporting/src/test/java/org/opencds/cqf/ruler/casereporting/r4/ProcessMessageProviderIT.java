package org.opencds.cqf.ruler.casereporting.r4;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.casereporting.CaseReportingConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { ProcessMessageProviderIT.class,
		CaseReportingConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
public class ProcessMessageProviderIT extends RestIntegrationTest {

	@Test
	public void testProcessMessage() throws IOException {

		String packagePrefix = "org/opencds/cqf/ruler/casereporting/r4/";

		Bundle bundle = (Bundle) loadResource(packagePrefix + "example-eicr.json");

		assertThrows(ResourceNotFoundException.class,
				() -> getClient().read().resource(Patient.class).withId("patient-12742542").execute());
		assertThrows(ResourceNotFoundException.class,
				() -> getClient().read().resource(Encounter.class).withId("encounter-97953898").execute());
		assertThrows(ResourceNotFoundException.class, () -> getClient().read().resource(Observation.class)
				.withId("78a5067f-5468-46ba-aaa5-429561e26acc").execute());
		assertThrows(ResourceNotFoundException.class, () -> getClient().read().resource(Observation.class)
				.withId("6deb9431-4fe1-41ae-9452-01a338d6da37").execute());
		assertThrows(ResourceNotFoundException.class, () -> getClient().read().resource(Observation.class)
				.withId("87f51021-9a31-44fc-a79b-d3339af7c749").execute());

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
				getClient().read().resource(Observation.class).withId("78a5067f-5468-46ba-aaa5-429561e26acc").execute());
		assertNotNull(
				getClient().read().resource(Observation.class).withId("6deb9431-4fe1-41ae-9452-01a338d6da37").execute());
		assertNotNull(
				getClient().read().resource(Observation.class).withId("87f51021-9a31-44fc-a79b-d3339af7c749").execute());
	}

}
