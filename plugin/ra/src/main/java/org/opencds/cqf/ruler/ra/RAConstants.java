package org.opencds.cqf.ruler.ra;

public class RAConstants {

	private RAConstants() {
	}

	// DaVinci IG constants
	public static final String REPORT_ID_SUFFIX = "-report";
	public static final String PATIENT_REPORT_PROFILE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-measurereport-bundle";

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
}
