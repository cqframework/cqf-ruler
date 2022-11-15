package org.opencds.cqf.ruler.ra.r4;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.ra.RAConfig;
import org.opencds.cqf.ruler.ra.RAConstants;
import org.opencds.cqf.ruler.ra.RAProperties;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.opencds.cqf.ruler.test.utility.Urls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.ruler.utility.r4.Parameters.parameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.stringPart;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = { ResolveProviderIT.class, RAConfig.class },
	properties = { "hapi.fhir.fhir_version=r4" })
class ResolveProviderIT extends RestIntegrationTest {
	@Autowired
	private RAProperties myRaProperties;

	@BeforeEach
	void beforeEach() {
		String ourServerBase = Urls.getUrl(myRaProperties.getReport().getEndpoint(), getPort());
		myRaProperties.getReport().setEndpoint(ourServerBase);
	}

	@Test
	void closureTest() {
		loadResource("Organization-ra-payer01.json");
		loadResource("Observation-ra-obs01pat02.json");
		loadResource("Encounter-ra-encounter31pat02.json");
		loadResource("Encounter-ra-measurereport03-remediate.json");
		loadResource("Condition-ra-condition31pat02.json");
		loadResource("Condition-ra-measurereport03-remediate.json");
		loadResource("Patient-ra-patient02.json");
		loadResource("MeasureReport-ra-measurereport03.json");
		loadResource("Bundle-ra-remediate-result.json");

		Parameters params = parameters(
			stringPart("periodStart", "2021-01-01"),
			stringPart("periodEnd", "2021-12-31"),
			stringPart("subject", "Patient/ra-patient02"));

		Parameters result = getClient().operation().onType(MeasureReport.class)
			.named("$ra.resolve-coding-gaps").withParameters(params)
			.useHttpGet().returnResourceType(Parameters.class).execute();

		assertFalse(result.isEmpty());
		assertTrue(result.hasParameter("return"));
		assertEquals(1, result.getParameter().size());
		assertTrue(result.getParameter().get(0).hasResource());
		assertTrue(result.getParameter().get(0).getResource() instanceof Bundle);

		// test that Composition status is set to final
		Bundle raBundle = (Bundle) result.getParameter().get(0).getResource();
		assertTrue(raBundle.hasEntry());
		assertTrue(raBundle.getEntryFirstRep().hasResource());
		assertTrue(raBundle.getEntryFirstRep().getResource() instanceof Composition);
		assertTrue(((Composition) raBundle.getEntryFirstRep().getResource()).hasStatus());
		assertEquals("final", ((Composition) raBundle.getEntryFirstRep().getResource()).getStatus().toCode());

		// Check that the MR group has been updated (closed)
		assertTrue(raBundle.getEntry().size() > 4);
		assertTrue(raBundle.getEntry().get(4).hasResource());
		assertTrue(raBundle.getEntry().get(4).getResource() instanceof MeasureReport);
		assertTrue(((MeasureReport) raBundle.getEntry().get(4).getResource()).hasGroup());
		assertTrue(((MeasureReport) raBundle.getEntry().get(4).getResource()).getGroup().get(1).hasId());
		assertEquals("group-002", ((MeasureReport) raBundle.getEntry().get(4).getResource()).getGroup().get(1).getId());
		assertTrue(((MeasureReport) raBundle.getEntry().get(4).getResource()).getGroup().get(1).hasExtension(RAConstants.EVIDENCE_STATUS_URL));
		Extension shouldBeClosed = ((MeasureReport) raBundle.getEntry().get(4).getResource()).getGroup().get(1).getExtensionByUrl(RAConstants.EVIDENCE_STATUS_URL);
		assertTrue(shouldBeClosed.hasValue() && shouldBeClosed.getValue() instanceof CodeableConcept);
		assertTrue(((CodeableConcept) shouldBeClosed.getValue()).hasCoding());
		assertTrue(((CodeableConcept) shouldBeClosed.getValue()).getCodingFirstRep().hasCode());
		assertEquals(RAConstants.CLOSED_GAP_CODE, ((CodeableConcept) shouldBeClosed.getValue()).getCodingFirstRep().getCode());

		assertEquals(11, raBundle.getEntry().size());
	}

	@Test
	void invalidationTest() {
		// TODO
	}

	@Test
	void creationTest() {
		// TODO
	}
}
