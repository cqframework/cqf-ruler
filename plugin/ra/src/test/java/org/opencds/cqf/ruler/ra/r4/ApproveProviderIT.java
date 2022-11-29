package org.opencds.cqf.ruler.ra.r4;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DetectedIssue;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.stringPart;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { ApproveProviderIT.class,
	RAConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
public class ApproveProviderIT extends RestIntegrationTest {

	@Autowired
	private RAProperties myRaProperties;

	@BeforeEach
	public void beforeEach() {
		String ourServerBase = Urls.getUrl(myRaProperties.getReport().getEndpoint(), getPort());
		myRaProperties.getReport().setEndpoint(ourServerBase);
	}

	@Test
	void testApproveProvider() {
		loadResource("Organization-ra-payer01.json");
		loadResource("Observation-ra-obs01pat02.json");
		loadResource("Encounter-ra-encounter31pat02.json");
		loadResource("Encounter-ra-measurereport03-remediate.json");
		loadResource("Condition-ra-condition31pat02.json");
		loadResource("Condition-ra-measurereport03-remediate.json");
		loadResource("Patient-ra-patient02.json");
		loadResource("Bundle-ra-remediate-result-closure.json");

		Parameters params = parameters(
			stringPart("periodStart", "2021-01-01"),
			stringPart("periodEnd", "2021-12-31"),
			stringPart("subject", "Patient/ra-patient02"),
			stringPart("measureId", "Measure-RAModelExample01"));

		Parameters result = getClient().operation().onType(MeasureReport.class)
			.named("$ra.approve-coding-gaps").withParameters(params)
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
		// check that DetectedIssue status' have been updated
		raBundle.getEntry().forEach(
			entry -> {
				if (entry.getResource() instanceof DetectedIssue) {
					DetectedIssue.DetectedIssueStatus status = ((DetectedIssue) entry.getResource()).getStatus();
					if (((DetectedIssue) entry.getResource()).getExtensionByUrl(RAConstants.GROUP_REFERENCE_URL)
							.getValue().primitiveValue().equals("group-001")) {
						assertSame(DetectedIssue.DetectedIssueStatus.FINAL, status);
					}
					else {
						assertTrue(status == DetectedIssue.DetectedIssueStatus.CANCELLED
							|| status == DetectedIssue.DetectedIssueStatus.FINAL);
					}
				}
			}
		);
	}
}
