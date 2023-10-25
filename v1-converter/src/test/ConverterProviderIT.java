package com.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
		ConverterConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
class ConverterProviderIT extends RestIntegrationTest {
	@Test
	void testConverterConfig() {
		var outcome = getClient()
				.operation()
				.onServer()
				.named("$convert-v1")
				.withNoParameters(Parameters.class)
				.returnResourceType(OperationOutcome.class)
				.execute();

		assertNotNull(outcome);
		assertEquals("Howdy", outcome.getIssueFirstRep().getDiagnostics());
	}
}
