package org.opencds.cqf.ruler.ra;

import java.util.Date;

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
	public static final String PATIENT_REPORT_PROFILE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-measurereport-bundle";
	public static final String CODING_GAP_BUNDLE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-coding-gap-bundle";
	public static final String MEASURE_REPORT_PROFILE_URL = "https://build.fhir.org/ig/HL7/davinci-ra/StructureDefinition-ra-measurereport.html";
	public static final String HCC_CODESYSTEM_URL = "http://terminology.hl7.org/CodeSystem/cmshcc";

	// Composition constants
	public static final Meta COMPOSITION_META = new Meta().addProfile(
			"http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-composition").setLastUpdated(new Date());
	public static final CodeableConcept COMPOSITION_TYPE = new CodeableConcept().addCoding(
			new Coding().setSystem("http://loinc.org").setCode("96315-7").setDisplay("Gaps in care report"));

	// DetectedIssue constants
	public static final String ORIGINAL_ISSUE_PROFILE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-coding-gap-original-detectedissue";
	public static final String CLINICAL_EVALUATION_ISSUE_PROFILE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-coding-gap-clinical-evaluation-detectedissue";
	public static final String GROUP_REFERENCE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-groupReference";
	public static final String CODING_GAP_REQUEST_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-codingGapRequest";
	public static final Extension CODING_GAP_TYPE_EXTENSION = new Extension().addExtension()
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
