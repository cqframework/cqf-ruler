package org.opencds.cqf.ruler.cr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KnowledgeArtifactProcessorTest {

	KnowledgeArtifactProcessor processor = new KnowledgeArtifactProcessor();

	@Test
	void testGetExpansionVSAC() throws IOException {
	    // given
		FhirContext ctx = FhirContext.forR4();

		String input = new String(this.getClass().getResourceAsStream("r4/valueset/valueset-2.16.840.1.113762.1.4.1116.89.json").readAllBytes());
		IParser parser = ctx.newJsonParser();
		ValueSet valueSet = parser.parseResource(ValueSet.class, input);

		// when
		processor.getExpansion(valueSet);
	   // then
      assertNotNull(valueSet.getExpansion());
	  	assertEquals(valueSet.getExpansion().getTotal(), 16);
	}

	@Test
	void testGetExpansionNaive() throws IOException {
		FhirContext ctx = FhirContext.forR4();

		String input = new String(this.getClass().getResourceAsStream("r4/valueset/valueset-anc-a-de13.json").readAllBytes());
		IParser parser = ctx.newJsonParser();
		ValueSet valueSet = parser.parseResource(ValueSet.class, input);

		// when
		processor.getExpansion(valueSet);
		// then
		assertNotNull(valueSet.getExpansion());
		assertNotNull(valueSet.getExpansion().getParameter().get(0));
		assertEquals(valueSet.getExpansion().getParameter().get(0).getName(), "naive");
		assertTrue(valueSet.getExpansion().getParameter().get(0).getValueBooleanType().booleanValue());
		assertEquals(valueSet.getExpansion().getContains().size(), 1);
		assertEquals(valueSet.getExpansion().getContains().get(0).getCode(), "ANC.A.DE13");
		assertEquals(valueSet.getExpansion().getContains().get(0).getDisplay(), "Co-habitants");
	}

}
