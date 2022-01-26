package org.opencds.cqf.ruler.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hl7.fhir.r4.model.CanonicalType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.utility.Canonicals.CanonicalParts;

public class CanonicalsTest {

	@Test
	public void fullCanonicalUrl() {
		String testUrl = "http://fhir.acme.com/Questionnaire/example|1.0#vs1";

		assertEquals("http://fhir.acme.com/Questionnaire/example", Canonicals.getUrl(testUrl));
		assertEquals("example", Canonicals.getIdPart(testUrl));
		assertEquals("1.0", Canonicals.getVersion(testUrl));
		assertEquals("vs1", Canonicals.getFragment(testUrl));
	}

	@Test
	public void partialCanonicalUrl() {
		String testUrl = "http://fhir.acme.com/Questionnaire/example";

		assertEquals("http://fhir.acme.com/Questionnaire/example", Canonicals.getUrl(testUrl));
		assertEquals("example", Canonicals.getIdPart(testUrl));
		assertNull(Canonicals.getVersion(testUrl));
		assertNull(Canonicals.getFragment(testUrl));
	}
	
	@Test
	public void fullCanonicalType() {
		CanonicalType testUrl = new CanonicalType("http://fhir.acme.com/Questionnaire/example|1.0#vs1");

		assertEquals("http://fhir.acme.com/Questionnaire/example", Canonicals.getUrl(testUrl));
		assertEquals("example", Canonicals.getIdPart(testUrl));
		assertEquals("1.0", Canonicals.getVersion(testUrl));
		assertEquals("vs1", Canonicals.getFragment(testUrl));
	}

	@Test
	public void partialCanonicalType() {
		CanonicalType  testUrl = new CanonicalType("http://fhir.acme.com/Questionnaire/example");
		assertEquals("http://fhir.acme.com/Questionnaire/example", Canonicals.getUrl(testUrl));
		assertEquals("Questionnaire", Canonicals.getResourceType(testUrl));
		assertEquals("example", Canonicals.getIdPart(testUrl));
		assertNull(Canonicals.getVersion(testUrl));
		assertNull(Canonicals.getFragment(testUrl));
	}

	@Test
	public void canonicalParts() {
		CanonicalType testUrl = new CanonicalType("http://fhir.acme.com/Questionnaire/example|1.0#vs1");

		CanonicalParts parts = Canonicals.getCanonicalParts(testUrl);

		assertEquals("http://fhir.acme.com/Questionnaire/example", parts.getUrl());
		assertEquals("Questionnaire", parts.getResourceType());
		assertEquals("example", Canonicals.getIdPart(testUrl), parts.getIdPart());
		assertEquals("1.0", Canonicals.getVersion(testUrl), parts.getVersion());
		assertEquals("vs1", Canonicals.getFragment(testUrl), parts.getFragment());
	}

}
