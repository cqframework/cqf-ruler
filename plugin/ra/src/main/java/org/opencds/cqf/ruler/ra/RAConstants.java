package org.opencds.cqf.ruler.ra;

import java.util.Collections;
import java.util.Date;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Meta;

public class RAConstants {

	private RAConstants() {
	}

	// DaVinci IG constants
	public static final String REPORT_ID_PREFIX = "coding-gaps-";
	public static final String REMEDIATE_ID_PREFIX = "remediate-coding-gaps-";
	public static final String RESOLVE_ID_PREFIX = "resolve-coding-gaps-";
	public static final String APPROVE_ID_PREFIX = "approve-coding-gaps-";
	public static final String PATIENT_REPORT_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-measurereport";
	public static final String CODING_GAP_BUNDLE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-coding-gap-bundle";
	public static final String SUSPECT_TYPE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-suspectType";
	public static final String EVIDENCE_STATUS_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-evidenceStatus";
	public static final String EVIDENCE_STATUS_DATE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-evidenceStatusDate";
	// Suspect Type
	public static final String SUSPECT_TYPE_SYSTEM = "http://hl7.org/fhir/us/davinci-ra/CodeSystem/suspect-type";
	public static final String HISTORIC_CODE = "historic";
	public static final String SUSPECTED_CODE = "suspected";
	public static final String NET_NEW_CODE = "net-new";
	public static final String HISTORIC_DISPLAY = "Historic Condition Category Gap";
	public static final String SUSPECTED_DISPLAY = "Suspected Condition Category Gap";
	public static final String NET_NEW_DISPLAY = "Net-New Condition Category Gap";
	public static final CodeableConcept HISTORIC_CONCEPT = new CodeableConcept().addCoding(new Coding()
			.setSystem(SUSPECT_TYPE_SYSTEM).setCode(HISTORIC_CODE).setDisplay(HISTORIC_DISPLAY));
	public static final CodeableConcept SUSPECTED_CONCEPT = new CodeableConcept().addCoding(new Coding()
			.setSystem(SUSPECT_TYPE_SYSTEM).setCode(SUSPECTED_CODE).setDisplay(SUSPECTED_DISPLAY));
	public static final CodeableConcept NET_NEW_CONCEPT = new CodeableConcept().addCoding(new Coding()
			.setSystem(SUSPECT_TYPE_SYSTEM).setCode(NET_NEW_CODE).setDisplay(NET_NEW_DISPLAY));
	public static final Extension SUSPECT_TYPE_HISTORIC_EXT = new Extension().setUrl(SUSPECT_TYPE_URL)
			.setValue(HISTORIC_CONCEPT);
	public static final Extension SUSPECT_TYPE_SUSPECTED_EXT = new Extension().setUrl(SUSPECT_TYPE_URL)
			.setValue(SUSPECTED_CONCEPT);
	public static final Extension SUSPECT_TYPE_NET_NEW_EXT = new Extension().setUrl(SUSPECT_TYPE_URL)
			.setValue(NET_NEW_CONCEPT);
	// Evidence Status
	public static final String EVIDENCE_STATUS_SYSTEM = "http://hl7.org/fhir/us/davinci-ra/CodeSystem/evidence-status";
	public static final String CLOSED_GAP_CODE = "closed-gap";
	public static final String OPEN_GAP_CODE = "open-gap";
	public static final String INVALID_GAP_CODE = "invalid-gap";
	public static final String CLOSED_GAP_DISPLAY = "Closed Condition Category Gap";
	public static final String OPEN_GAP_DISPLAY = "Open Condition Category Gap";
	public static final String INVALID_GAP_DISPLAY = "Invalid Condition Category Gap";
	public static final CodeableConcept CLOSED_GAP_CONCEPT = new CodeableConcept().addCoding(new Coding()
			.setSystem(EVIDENCE_STATUS_SYSTEM).setCode(CLOSED_GAP_CODE).setDisplay(CLOSED_GAP_DISPLAY));
	public static final CodeableConcept OPEN_GAP_CONCEPT = new CodeableConcept().addCoding(new Coding()
			.setSystem(EVIDENCE_STATUS_SYSTEM).setCode(OPEN_GAP_CODE).setDisplay(OPEN_GAP_DISPLAY));
	public static final CodeableConcept INVALID_GAP_CONCEPT = new CodeableConcept().addCoding(new Coding()
			.setSystem(EVIDENCE_STATUS_SYSTEM).setCode(INVALID_GAP_CODE).setDisplay(INVALID_GAP_DISPLAY));
	public static final Extension EVIDENCE_STATUS_CLOSED_EXT = new Extension().setUrl(EVIDENCE_STATUS_URL)
			.setValue(CLOSED_GAP_CONCEPT);
	public static final Extension EVIDENCE_STATUS_OPEN_EXT = new Extension().setUrl(EVIDENCE_STATUS_URL)
			.setValue(OPEN_GAP_CONCEPT);
	public static final Extension EVIDENCE_STATUS_INVALID_EXT = new Extension().setUrl(EVIDENCE_STATUS_URL)
			.setValue(INVALID_GAP_CONCEPT);

	// MeasureReport constants
	public static final Meta PATIENT_REPORT_META = new Meta().setProfile(
			Collections.singletonList(new CanonicalType(RAConstants.PATIENT_REPORT_URL)))
			.setLastUpdated(new Date());

	// Bundle constants
	public static final Meta CODING_GAP_REPORT_BUNDLE_META = new Meta().setProfile(
			Collections.singletonList(new CanonicalType(RAConstants.CODING_GAP_BUNDLE_URL)))
			.setLastUpdated(new Date());

	// Composition constants
	public static final Meta COMPOSITION_META = new Meta().addProfile(
			"http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-coding-gap-composition")
			.setLastUpdated(new Date());
	public static final CodeableConcept COMPOSITION_TYPE = new CodeableConcept().addCoding(
			new Coding().setSystem("http://loinc.org").setCode("96315-7").setDisplay("Gaps in care report"));

	// DetectedIssue constants
	public static final String ORIGINAL_ISSUE_PROFILE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-coding-gap-original-detectedissue";
	public static final String CLINICAL_EVALUATION_ISSUE_PROFILE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-coding-gap-clinical-evaluation-detectedissue";
	public static final String GROUP_REFERENCE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-groupReference";
	public static final String CODING_GAP_REQUEST_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-codingGapRequest";
	public static final Extension CODING_GAP_REQUEST_EXTENSION = new Extension().addExtension()
			.setUrl(CODING_GAP_REQUEST_URL)
			.setValue(
					new CodeableConcept().addCoding(
							new Coding().setSystem("http://hl7.org/fhir/us/davinci-ra/CodeSystem/coding-gap-type")
									.setCode("payer-generated")));
	public static final CodeableConcept CODING_GAP_CODE = new CodeableConcept().addCoding(
			new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode").setCode("CODINGGAP")
					.setDisplay("Codinggap"));

	// Parameter validation constants
	public static final String INVALID_PARAMETERS_NAME = "Invalid parameters";
	public static final String INVALID_PARAMETERS_SEVERITY = "error";

	// Operation parameter name constants
	public static final String PERIOD_START = "periodStart";
	public static final String PERIOD_END = "periodEnd";
	public static final String SUBJECT = "subject";
	public static final String MEASURE_ID = "measureId";
	public static final String MEASURE_IDENTIFIER = "measureIdentifier";
	public static final String MEASURE_URL = "measureUrl";
	public static final String RETURN_PARAM_NAME = "return";
}
