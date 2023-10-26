package com.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
		ConverterConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
class ConverterProviderIT extends RestIntegrationTest {
	@Test
	void testConverterConfig() {
		loadResource("ersd-v1-plandefinition-skeleton.json");
		Bundle v2Bundle = (Bundle) loadResource("ersd-bundle-example.json");
		
		Bundle outcome = getClient()
				.operation()
				.onServer()
				.named("$convert-v1")
				.withParameters(v2BundleParams)
				.returnResourceType(Bundle.class)
				.execute();

		assertNotNull(outcome);
		assertEquals("hi", outcome.getIssueFirstRep().getDiagnostics());
	}
}
