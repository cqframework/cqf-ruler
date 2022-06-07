package org.opencds.cqf.ruler.atr.util;

public final class TextConstants {

	private TextConstants() {}

	public static final String SINGLE_FORWARD_SLASH = "/";

	public static final String MEMBER_ID = "memberId";

	public static final String PROVIDER_NPI = "providerNpi";

	public static final String PATIENT_REFERENCE = "patientReference";

	public static final String PROVIDER_REFERENCE = "providerReference";

	public static final String ATTRIBUTION_PERIOD = "attributionPeriod";

	public static final String MEMBER_CHANGETYPE_SYSTEM =
			"http://hl7.org/fhir/us/davinci-atr/StructureDefinition/ext-changeType";

	public static final String MEMBER_COVERAGE_SYSTEM =
			"http://hl7.org/fhir/us/davinci-atr/StructureDefinition/ext-coverageReference";

	public static final String MEMBER_PROVIDER_SYSTEM =
			"http://hl7.org/fhir/us/davinci-atr/StructureDefinition/ext-attributedProvider";

	public static final String NEW_TYPE = "new";

	public static final String CHANGE_TYPE = "change";

	public static final String NOCHANGE_TYPE = "nochange";

	/** The Constant HTTP_POST. */
	public static final String HTTP_POST = "POST";
}
