package org.opencds.cqf.ruler.cr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@Lazy
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {KnowledgeArtifactProcessorIT.class, CrConfig.class},
	properties = {"hapi.fhir.fhir_version=r4", "hapi.fhir.security.basic_auth.enabled=false"})
public class KnowledgeArtifactProcessorIT extends RestIntegrationTest {

	@Autowired
	KnowledgeArtifactProcessor processor;

	@Autowired
	TerminologyServerClient client;

	@Disabled
	@Test
	void testGetExpansionVSAC() throws IOException {
	    // given
		FhirContext ctx = FhirContext.forR4();

		String input = new String(this.getClass().getResourceAsStream("r4/valueset/valueset-2.16.840.1.113762.1.4.1116.89.json").readAllBytes());
		IParser parser = ctx.newJsonParser();
		ValueSet valueSet = parser.parseResource(ValueSet.class, input);
		String codeSystemVersion = valueSet.getCompose().getInclude().get(0).getVersion();

		Parameters expansionParameters = new Parameters();

		// when
		processor.expandValueSet(valueSet, expansionParameters);

	   // then
      assertNotNull(valueSet.getExpansion());
	  	assertEquals(16, valueSet.getExpansion().getTotal());
	}

}
