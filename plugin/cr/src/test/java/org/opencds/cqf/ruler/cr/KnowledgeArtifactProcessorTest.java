package org.opencds.cqf.ruler.cr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class KnowledgeArtifactProcessorTest {

	@InjectMocks
	KnowledgeArtifactProcessor processor;

	@Mock
	TerminologyServerClient client;

	@Test
	void testGetExpansionVSAC() throws IOException {
	    // given
		FhirContext ctx = FhirContext.forR4();

		String input = new String(this.getClass().getResourceAsStream("r4/valueset/valueset-2.16.840.1.113762.1.4.1116.89.json").readAllBytes());
		IParser parser = ctx.newJsonParser();
		ValueSet valueSet = parser.parseResource(ValueSet.class, input);

		Parameters expansionParameters = new Parameters();
		expansionParameters.addParameter("system-version", "http://snomed.info/sct|http://snomed.info/sct/731000124108/version/20230901");

		// when
		String expandedValueSetString = new String(this.getClass().getResourceAsStream("r4/test/valueset-expanded.json").readAllBytes());
		ValueSet expandedValueSet = parser.parseResource(ValueSet.class, expandedValueSetString);
		Mockito.when(client.expand(Mockito.eq(valueSet), Mockito.eq(valueSet.getUrl()), Mockito.eq(expansionParameters))).thenReturn(expandedValueSet);

		processor.expandValueSet(valueSet, expansionParameters);

	   // then
      assertNotNull(valueSet.getExpansion());
	  	assertEquals(16, valueSet.getExpansion().getTotal());
	}

	@Test
	void testGetExpansionNaive() throws IOException {
		FhirContext ctx = FhirContext.forR4();

		String input = new String(this.getClass().getResourceAsStream("r4/valueset/valueset-vsm-authored.json").readAllBytes());
		IParser parser = ctx.newJsonParser();
		ValueSet valueSet = parser.parseResource(ValueSet.class, input);
		Parameters expansionParameters = new Parameters();
		expansionParameters.addParameter("system-version", "http://snomed.info/sct|http://snomed.info/sct/731000124108/version/20230901");

		// when
		processor.expandValueSet(valueSet, expansionParameters);

		// then
		assertNotNull(valueSet.getExpansion());
		assertNotNull(valueSet.getExpansion().getParameter().get(0));
		assertEquals("naive", valueSet.getExpansion().getParameter().get(0).getName());
		assertTrue(valueSet.getExpansion().getParameter().get(0).getValueBooleanType().booleanValue());
		assertEquals(1, valueSet.getExpansion().getContains().size());
		assertEquals("ANC.A.DE13", valueSet.getExpansion().getContains().get(0).getCode());
		assertEquals("Co-habitants", valueSet.getExpansion().getContains().get(0).getDisplay());
	}
}
