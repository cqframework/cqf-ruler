package org.opencds.cqf.ruler.ra.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.stringPart;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DetectedIssue;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.ra.RAConfig;
import org.opencds.cqf.ruler.ra.RAProperties;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.opencds.cqf.ruler.test.utility.Urls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
		RAConfig.class }, properties = { "hapi.fhir.fhir_version=r4", "hapi.fhir.cr.enabled=true" })
class RemediateProviderIT extends RestIntegrationTest {

	@Autowired
	private RAProperties myRaProperties;

	@BeforeEach
	public void beforeEach() {
		String ourServerBase = Urls.getUrl(myRaProperties.getReport().getEndpoint(), getPort());
		myRaProperties.getReport().setEndpoint(ourServerBase);
	}

	@Test
	void testRemediateProvider() {
		loadResource("Organization-ra-payer01.json");
		loadResource("Observation-ra-obs01pat02.json");
		loadResource("Encounter-ra-encounter31pat02.json");
		loadResource("Encounter-ra-measurereport03-remediate.json");
		loadResource("Condition-ra-condition31pat02.json");
		loadResource("Condition-ra-measurereport03-remediate.json");
		loadResource("Patient-ra-patient02.json");
		loadResource("MeasureReport-ra-measurereport03.json");
		// least recent
		loadResource("Bundle-ra-coding-gaps-result-1.json");
		// most recent
		loadResource("Bundle-ra-coding-gaps-result-2.json");
		// in-between recent
		loadResource("Bundle-ra-coding-gaps-result-3.json");
		loadResource("DetectedIssue-ra-measurereport03-remediate.json");

		Parameters params = parameters(
				stringPart("periodStart", "2021-01-01"),
				stringPart("periodEnd", "2021-12-31"),
				stringPart("subject", "Patient/ra-patient02"));

		Parameters result = getClient().operation().onType(MeasureReport.class)
				.named("$ra.remediate-coding-gaps").withParameters(params)
				.useHttpGet().returnResourceType(Parameters.class).execute();

		assertFalse(result.isEmpty());
		assertTrue(result.hasParameter("return"));
		assertEquals(1, result.getParameter().size());
		assertTrue(result.getParameter().get(0).hasResource());
		assertTrue(result.getParameter().get(0).getResource() instanceof Bundle);

		Bundle raBundle = (Bundle) result.getParameter().get(0).getResource();
		// check for document Bundle type
		assertTrue(raBundle.hasType());
		assertEquals(Bundle.BundleType.DOCUMENT, raBundle.getType());
		// check that first entry is Composition
		assertTrue(raBundle.hasEntry());
		assertTrue(raBundle.getEntryFirstRep().hasResource());
		assertTrue(raBundle.getEntryFirstRep().getResource() instanceof Composition);
		// check that Composition identifier has not changed
		assertTrue(((Composition) raBundle.getEntryFirstRep().getResource()).hasIdentifier());
		assertTrue(((Composition) raBundle.getEntryFirstRep().getResource()).getIdentifier().hasValue());
		assertEquals("urn:uuid:e729e44f-756b-43cd-a9a3-2913d3bce01d",
				((Composition) raBundle.getEntryFirstRep().getResource()).getIdentifier().getValue());
		// check that Composition date has not changed
		assertTrue(((Composition) raBundle.getEntryFirstRep().getResource()).hasDate());
		assertEquals("2022-11-06T15:50:24-06:00",
				((Composition) raBundle.getEntryFirstRep().getResource()).getDateElement().getValueAsString());
		// check that new DetectedIssue was added to Composition (section size was 2 ->
		// should now be 3)
		assertTrue(((Composition) raBundle.getEntryFirstRep().getResource()).hasSection());
		assertEquals(3, ((Composition) raBundle.getEntryFirstRep().getResource()).getSection().size());
		// check that next three entries are DetectedIssues
		assertTrue(raBundle.getEntry().size() > 1);
		assertTrue(raBundle.getEntry().get(1).hasResource());
		assertTrue(raBundle.getEntry().get(1).getResource() instanceof DetectedIssue);
		assertTrue(raBundle.getEntry().size() > 2);
		assertTrue(raBundle.getEntry().get(2).hasResource());
		assertTrue(raBundle.getEntry().get(2).getResource() instanceof DetectedIssue);
		assertTrue(raBundle.getEntry().size() > 3);
		assertTrue(raBundle.getEntry().get(3).hasResource());
		assertTrue(raBundle.getEntry().get(3).getResource() instanceof DetectedIssue);
		// check that MeasureReport follows issues
		assertTrue(raBundle.getEntry().size() > 4);
		assertTrue(raBundle.getEntry().get(4).hasResource());
		assertTrue(raBundle.getEntry().get(4).getResource() instanceof MeasureReport);
		assertEquals(12, raBundle.getEntry().size());
	}
}
