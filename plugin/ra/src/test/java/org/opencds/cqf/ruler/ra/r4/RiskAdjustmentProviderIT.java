package org.opencds.cqf.ruler.ra.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newPart;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.behavior.r4.MeasureReportUser;
import org.opencds.cqf.ruler.ra.RAConfig;
import org.opencds.cqf.ruler.ra.RAProperties;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.opencds.cqf.ruler.test.utility.Urls;
import org.opencds.cqf.ruler.utility.Ids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { RiskAdjustmentProviderIT.class,
		RAConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
class RiskAdjustmentProviderIT extends RestIntegrationTest {
	@Autowired
	private RAProperties myRaProperties;

	@BeforeEach
	void beforeEach() {
		String ourServerBase = Urls.getUrl(myRaProperties.getReport().getEndpoint(), getPort());
		myRaProperties.getReport().setEndpoint(ourServerBase);
		loadTransaction("ConditionCategoryPOC-bundle.json");
	}

	@Test
	void riskAssessmentHistoricOpen() {
		loadTransaction("tests-hist-open-HCC189-bundle.json");

		Parameters resultParams = callOperation(getRequestParameters("Patient/hist-open-HCC189"));
		validateResultParameters(resultParams, "Patient/hist-open-HCC189");

		Bundle raBundle = (Bundle) resultParams.getParameterFirstRep().getResource();
		validateBundle(raBundle);

		MeasureReport response = (MeasureReport) raBundle.getEntryFirstRep().getResource();
		validateMeasureReport(response, "historic", "open-gap", "2020-01-31");
	}

	@Test
	void riskAssessmentHistoricClosed() {
		loadTransaction("tests-hist-closed-HCC189-bundle.json");

		Parameters resultParams = callOperation(getRequestParameters("Patient/hist-closed-HCC189"));
		validateResultParameters(resultParams, "Patient/hist-closed-HCC189");

		Bundle raBundle = (Bundle) resultParams.getParameterFirstRep().getResource();
		validateBundle(raBundle);

		MeasureReport response = (MeasureReport) raBundle.getEntryFirstRep().getResource();
		validateMeasureReport(response, "historic", "closed-gap", "2022-01-31");
	}

	@Test
	void riskAssessmentHistoricNetNew() {
		loadTransaction("tests-netnew-HCC189-bundle.json");

		Parameters resultParams = callOperation(getRequestParameters("Patient/netnew-HCC189"));
		validateResultParameters(resultParams, "Patient/netnew-HCC189");

		Bundle raBundle = (Bundle) resultParams.getParameterFirstRep().getResource();
		validateBundle(raBundle);

		MeasureReport response = (MeasureReport) raBundle.getEntryFirstRep().getResource();
		validateMeasureReport(response, "net-new", "closed-gap", "2022-01-31");
	}

	private Parameters getRequestParameters(String subject) {
		return newParameters(
				newPart("periodStart", "2022-01-01"),
				newPart("periodEnd", "2022-12-31"),
				newPart("subject", subject),
				newPart("type", "report"));
	}

	private Parameters callOperation(Parameters requestParameters) {
		return getClient().operation()
				.onInstance(new IdType("Measure", "ConditionCategoryPOC"))
				.named("$evaluate-risk-condition-category")
				.withParameters(requestParameters)
				.returnResourceType(Parameters.class)
				.execute();
	}

	private void validateResultParameters(Parameters resultParams, String patientReference) {
		assertTrue(resultParams.hasParameter());
		assertEquals(1, resultParams.getParameter().size());
		assertTrue(resultParams.getParameterFirstRep().hasName());
		assertEquals(patientReference, resultParams.getParameterFirstRep().getName());
		assertTrue(resultParams.getParameterFirstRep().hasResource());
		assertTrue(resultParams.getParameterFirstRep().getResource() instanceof Bundle);
	}

	private void validateBundle(Bundle riskAssessmentBundle) {
		assertTrue(riskAssessmentBundle.hasEntry());
		List<String> bundleResourceReferences = riskAssessmentBundle.getEntry().stream()
				.map(entry -> entry.getResource().hasIdElement() ? Ids.simple(entry.getResource().getIdElement()) : null)
				.collect(Collectors.toList());
		validateUniqueBundleEntry(bundleResourceReferences);
		assertTrue(riskAssessmentBundle.getEntryFirstRep().getResource() instanceof MeasureReport);
		MeasureReport raBundle = (MeasureReport) riskAssessmentBundle.getEntryFirstRep().getResource();
		validateEvalResourcesInBundle(bundleResourceReferences, raBundle.getEvaluatedResource().stream()
				.map(Reference::getReference).collect(Collectors.toList()));
		validateSdeInBundle(bundleResourceReferences, raBundle.getExtension().stream()
				.filter(extension -> extension.getUrl().equals(
						MeasureReportUser.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_URL))
				.map(extension -> extension.getValue().toString()).collect(Collectors.toList()));
	}

	private void validateUniqueBundleEntry(List<String> entryReferences) {
		assertTrue(entryReferences.stream().allMatch(new HashSet<String>()::add));
	}

	private void validateEvalResourcesInBundle(List<String> bundleResourceRefs, List<String> evaluatedResourceRefs) {
		assertTrue(bundleResourceRefs.containsAll(evaluatedResourceRefs));
	}

	private void validateSdeInBundle(List<String> bundleResourceReferences, List<String> sdeResourceReferences) {
		assertTrue(bundleResourceReferences.containsAll(sdeResourceReferences));
	}

	private void validateMeasureReport(MeasureReport response, String suspectType,
			String evidenceStatus, String evidenceStatusDate) {
		assertFalse(response.getGroup().isEmpty());
		assertEquals(1, response.getGroup().size());
		assertFalse(response.getGroupFirstRep().getExtension().isEmpty());
		assertEquals(3, response.getGroupFirstRep().getExtension().size());
		assertTrue(response.getGroupFirstRep().getExtensionFirstRep().hasValue());
		assertTrue(response.getGroupFirstRep().getExtensionFirstRep().getValue() instanceof CodeableConcept
				&& ((CodeableConcept) response.getGroupFirstRep().getExtensionFirstRep().getValue()).hasCoding());
		assertEquals("http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-suspectType",
				((CodeableConcept) response.getGroupFirstRep().getExtensionFirstRep().getValue()).getCodingFirstRep()
						.getSystem());
		assertEquals(suspectType, ((CodeableConcept) response.getGroupFirstRep().getExtensionFirstRep().getValue())
				.getCodingFirstRep().getCode());
		assertTrue(response.getGroupFirstRep().getExtension().get(1).getValue() instanceof CodeableConcept
				&& ((CodeableConcept) response.getGroupFirstRep().getExtension().get(1).getValue()).hasCoding());
		assertEquals("http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-evidenceStatus",
				((CodeableConcept) response.getGroupFirstRep().getExtension().get(1).getValue()).getCodingFirstRep()
						.getSystem());
		assertEquals(evidenceStatus, ((CodeableConcept) response.getGroupFirstRep().getExtension().get(1).getValue())
				.getCodingFirstRep().getCode());
		assertTrue(response.getGroupFirstRep().getExtension().get(2).getValue() instanceof CodeableConcept
				&& ((CodeableConcept) response.getGroupFirstRep().getExtension().get(2).getValue()).hasCoding());
		assertEquals("http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-evidenceStatusDate",
				((CodeableConcept) response.getGroupFirstRep().getExtension().get(2).getValue()).getCodingFirstRep()
						.getSystem());
		assertEquals(evidenceStatusDate, ((CodeableConcept) response.getGroupFirstRep().getExtension().get(2).getValue())
				.getCodingFirstRep().getCode());
		assertTrue(response.getGroupFirstRep().hasCode() && response.getGroupFirstRep().getCode().hasCoding());
		assertTrue(response.getGroupFirstRep().getCode().getCodingFirstRep().hasSystem());
		assertEquals("http://terminology.hl7.org/CodeSystem/cmshcc",
				response.getGroupFirstRep().getCode().getCodingFirstRep().getSystem());
		assertTrue(response.getGroupFirstRep().getCode().getCodingFirstRep().hasCode());
		assertEquals("189", response.getGroupFirstRep().getCode().getCodingFirstRep().getCode());
		assertTrue(response.getGroupFirstRep().getCode().getCodingFirstRep().hasDisplay());
		assertEquals("Amputation Status, Lower Limb/Amputation Complications",
				response.getGroupFirstRep().getCode().getCodingFirstRep().getDisplay());
	}
}
