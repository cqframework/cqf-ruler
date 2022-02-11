package org.opencds.cqf.ruler.cr.r4.provider;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.common.base.Strings;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.cqf.ruler.behavior.ResourceCreator;
import org.opencds.cqf.ruler.behavior.r4.ParameterUser;
import org.opencds.cqf.ruler.builder.BundleBuilder;
import org.opencds.cqf.ruler.builder.BundleSettings;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.Operations;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class CareGapsProvider extends DaoRegistryOperationProvider implements ParameterUser, ResourceCreator {

	public static final Pattern CARE_GAPS_STATUS = Pattern
			.compile("(open-gap|closed-gap|not-applicable)");
	public static final String CARE_GAPS_BUNDLE_PROFILE = "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/gaps-bundle-deqm";

	/**
	 * Implements the <a href=
	 * "http://build.fhir.org/ig/HL7/davinci-deqm/OperationDefinition-care-gaps.html">$care-gaps</a>
	 * operation found in the
	 * <a href="http://build.fhir.org/ig/HL7/davinci-deqm/index.html">Da Vinci DEQM
	 * FHIR Implementation Guide</a> that overrides the <a href=
	 * "http://build.fhir.org/operation-measure-care-gaps.html">$care-gaps</a>
	 * operation found in the
	 * <a href="http://hl7.org/fhir/R4/clinicalreasoning-module.html">FHIR Clinical
	 * Reasoning Module</a>.
	 * 
	 * The operation calculates measures describing gaps in care. For more details,
	 * reference the <a href=
	 * "http://build.fhir.org/ig/HL7/davinci-deqm/gaps-in-care-reporting.html">Gaps
	 * in Care Reporting</a> section of the
	 * <a href="http://build.fhir.org/ig/HL7/davinci-deqm/index.html">Da Vinci DEQM
	 * FHIR Implementation Guide</a>.
	 * 
	 * A Parameters resource that includes zero to many document bundles that
	 * include Care Gap Measure Reports will be returned.
	 * 
	 * Usage:
	 * URL: [base]/Measure/$care-gaps
	 * 
	 * @param theRequestDetails generally auto-populated by the HAPI server
	 *                          framework.
	 * @param periodStart       the start of the gaps through period
	 * @param periodEnd         the end of the gaps through period
	 * @param topic             the category of the measures that is of interest for
	 *                          the care gaps report
	 * @param subject           a reference to either a Patient or Group for which
	 *                          the gaps in care report(s) will be generated
	 * @param practitioner      a reference to a Practitioner for which the gaps in
	 *                          care report(s) will be generated
	 * @param organization      a reference to an Organization for which the gaps in
	 *                          care report(s) will be generated
	 * @param status            the status code of gaps in care reports that will be
	 *                          included in the result
	 * @param measureId         the id of Measure(s) for which the gaps in care
	 *                          report(s) will be calculated
	 * @param measureIdentifier the identifier of Measure(s) for which the gaps in
	 *                          care report(s) will be calculated
	 * @param measureUrl        the canonical URL of Measure(s) for which the gaps
	 *                          in care report(s) will be calculated
	 * @param program           the program that a provider (either clinician or
	 *                          clinical organization) participates in
	 * @return Parameters of bundles of Care Gap Measure Reports
	 */
	@SuppressWarnings("squid:S00107") // warning for greater than 7 parameters
	@Description(shortDefinition = "$care-gaps", value = "Implements the <a href=\"http://build.fhir.org/ig/HL7/davinci-deqm/OperationDefinition-care-gaps.html\">$care-gaps</a> operation found in the <a href=\"http://build.fhir.org/ig/HL7/davinci-deqm/index.html\">Da Vinci DEQM FHIR Implementation Guide</a> which is an extension of the <a href=\"http://build.fhir.org/operation-measure-care-gaps.html\">$care-gaps</a> operation found in the <a href=\"http://hl7.org/fhir/R4/clinicalreasoning-module.html\">FHIR Clinical Reasoning Module</a>.")
	@Operation(name = "$care-gaps", idempotent = true, type = Measure.class)
	public Parameters careGapsReport(RequestDetails theRequestDetails,
			@OperationParam(name = "periodStart") String periodStart,
			@OperationParam(name = "periodEnd") String periodEnd,
			@OperationParam(name = "topic") List<String> topic,
			@OperationParam(name = "subject") String subject,
			@OperationParam(name = "practitioner") String practitioner,
			@OperationParam(name = "organization") String organization,
			@OperationParam(name = "status") List<String> status,
			@OperationParam(name = "measureId") List<String> measureId,
			@OperationParam(name = "measureIdentifier") List<String> measureIdentifier,
			@OperationParam(name = "measureUrl") List<CanonicalType> measureUrl,
			@OperationParam(name = "program") List<String> program) {

		/*
		 * TODO - topic should allow many and be a union of them
		 * TODO -
		 * "The Server needs to make sure that practitioner is authorized to get the gaps in care report for and know what measures the practitioner are eligible or qualified."
		 */

		validateParameters(theRequestDetails);
		Parameters result = newResource(Parameters.class, "care-gaps-report-" + UUID.randomUUID().toString());
		List<Measure> measures = getMeasures(measureId, measureIdentifier, measureUrl);

		if (!Strings.isNullOrEmpty(subject)) {
			List<Patient> patients = getPatientListFromSubject(subject);
			(patients)
					.forEach(
							patient -> {
								Parameters.ParametersParameterComponent patientParameter = patientReport(periodStart, periodEnd,
										topic, patient, status, measures, organization);
								result.addParameter(patientParameter);
							});
		} else {
			// TODO: implement non subject parameters (practitioner and organization)
			throw new NotImplementedException("Non subject parameters have not been implemented.");
		}

		return result;
	}

	public void validateParameters(RequestDetails theRequestDetails) {
		Operations.validatePeriod(theRequestDetails, "periodStart", "periodEnd");
		Operations.validateCardinality(theRequestDetails, "subject", 0, 1);
		Operations.validateSingularPattern(theRequestDetails, "subject", Operations.PATIENT_OR_GROUP_REFERENCE);
		Operations.validateCardinality(theRequestDetails, "status", 1);
		Operations.validateSingularPattern(theRequestDetails, "status", CARE_GAPS_STATUS);
		Operations.validateExclusive(theRequestDetails, "subject", "organization", "practitioner");
		Operations.validateExclusive(theRequestDetails, "organization", "subject");
		Operations.validateInclusive(theRequestDetails, "practitioner", "organization");
		Operations.validateExclusiveOr(theRequestDetails, "subject", "organization");
		Operations.validateAtLeastOne(theRequestDetails, "measureId", "measureIdentifier", "measureUrl");
	}

	private Parameters.ParametersParameterComponent patientReport(String periodStart, String periodEnd,
			List<String> topic, Patient patient, List<String> status, List<Measure> measures, String organization) {
		Parameters.ParametersParameterComponent patientParameter = new Parameters.ParametersParameterComponent();

		// subject.replace("/", "-") +

		Bundle bundle = (Bundle) BundleBuilder.create(Bundle.class,
				(BundleSettings) new BundleSettings().addType(BundleType.DOCUMENT.toString())
						.addProfile(CARE_GAPS_BUNDLE_PROFILE).withDefaults());

		return patientParameter;
	}
}
