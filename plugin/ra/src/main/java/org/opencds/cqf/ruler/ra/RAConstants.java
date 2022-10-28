package org.opencds.cqf.ruler.ra;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;

public class RAConstants {

	private RAConstants() {
	}

	// DaVinci IG constants
	public static final String REPORT_ID_SUFFIX = "-report";
	public static final String PATIENT_REPORT_PROFILE_URL =
		"http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-measurereport-bundle";
	public static final String CODING_GAP_BUNDLE_URL =
		"http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-coding-gap-bundle";

	// Composition constants
	public static final Meta COMPOSITION_META = new Meta().addProfile(
		"http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-composition");
	public static final CodeableConcept COMPOSITION_TYPE = new CodeableConcept().addCoding(
		new Coding().setSystem("http://loinc.org").setCode("96315-7").setDisplay("Gaps in care report"));

	// DetectedIssue constants
	public static final String ORIGINAL_ISSUE_PROFILE_URL =
		"http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-coding-gap-original-detectedissue";

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
