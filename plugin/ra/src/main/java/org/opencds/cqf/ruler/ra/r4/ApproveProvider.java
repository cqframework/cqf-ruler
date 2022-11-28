package org.opencds.cqf.ruler.ra.r4;

import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DetectedIssue;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.behavior.ResourceCreator;
import org.opencds.cqf.ruler.behavior.r4.ParameterUser;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.ra.RAConstants;
import org.opencds.cqf.ruler.utility.Operations;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class ApproveProvider extends DaoRegistryOperationProvider
		implements ParameterUser, ResourceCreator, RiskAdjustmentUser {

	/**
	 * Implements the <a href=
	 * "https://build.fhir.org/ig/HL7/davinci-ra/OperationDefinition-ra.approve-coding-gaps.html">$ra.approve-coding-gaps</a>
	 * operation found in the
	 * <a href="https://build.fhir.org/ig/HL7/davinci-ra/index.html">Da Vinci Risk
	 * Adjustment IG</a>.
	 *
	 * @param requestDetails    metadata about the current request being processed.
	 *                          Generally auto-populated by the HAPI FHIR server
	 *                          framework.
	 * @param periodStart       the start of the clinical evaluation period
	 * @param periodEnd         the end of the clinical evaluation period
	 * @param subject           a Patient or Patient Group
	 * @param measureId         the id of a Measure resource
	 * @param measureIdentifier the identifier of a Measure resource
	 * @param measureUrl        the url of a Measure resource
	 * @return a Parameters with <a href=
	 *         "http://build.fhir.org/ig/HL7/davinci-ra/StructureDefinition-ra-coding-gap-bundle.html">Risk
	 *         Adjustment Coding Gap Bundles</a>
	 */
	@Description(shortDefinition = "$ra.approve-coding-gaps operation", value = "Implements the <a href=\"https://build.fhir.org/ig/HL7/davinci-ra/OperationDefinition-ra.approve-coding-gaps.html\">$ra.approve-coding-gaps</a> operation found in the <a href=\"https://build.fhir.org/ig/HL7/davinci-ra/index.html\">Da Vinci Risk Adjustment IG</a>.")
	@Operation(name = "$ra.approve-coding-gaps", idempotent = true, type = MeasureReport.class)
	public Parameters approve(
			RequestDetails requestDetails,
			@OperationParam(name = RAConstants.PERIOD_START, typeName = "date") IPrimitiveType<Date> periodStart,
			@OperationParam(name = RAConstants.PERIOD_END, typeName = "date") IPrimitiveType<Date> periodEnd,
			@OperationParam(name = RAConstants.SUBJECT) String subject,
			@OperationParam(name = RAConstants.MEASURE_ID) List<String> measureId,
			@OperationParam(name = RAConstants.MEASURE_IDENTIFIER) List<String> measureIdentifier,
			@OperationParam(name = RAConstants.MEASURE_URL) List<String> measureUrl) throws FHIRException {
		try {
			validateParameters(requestDetails);
		} catch (Exception e) {
			return parameters(part(RAConstants.INVALID_PARAMETERS_NAME,
					generateIssue(RAConstants.INVALID_PARAMETERS_SEVERITY, e.getMessage())));
		}

		List<Bundle> codingGapReportBundles = new ArrayList<>();
		getPatientListFromSubject(subject).forEach(
				patient -> {
					Bundle b = getMostRecentCodingGapReportBundle(subject, normalizeMeasureReference(
							measureId, measureIdentifier, measureUrl), periodStart.getValue(), periodEnd.getValue());
					MeasureReport mr = getReportFromBundle(b);
					Composition composition = getCompositionFromBundle(b);
					List<DetectedIssue> issues = getMostRecentIssuesFromBundle(b);
					Resource author = getAuthorFromBundle(b, composition);
					for (int i = 0; i < issues.size(); ++i) {
						if (i == 0) {
							issues.get(i).setStatus(DetectedIssue.DetectedIssueStatus.FINAL);
						} else {
							issues.get(i).setStatus(DetectedIssue.DetectedIssueStatus.CANCELLED);
						}
						issues.get(i).getMeta().setLastUpdated(new Date());
					}
					codingGapReportBundles.add(
							buildCodingGapReportBundle(requestDetails.getFhirServerBase(), composition, issues, mr, author));
				});

		Parameters result = newResource(Parameters.class, RAConstants.APPROVE_ID_PREFIX + UUID.randomUUID());

		for (Bundle codingGapReportBundle : codingGapReportBundles) {
			result.addParameter(part(RAConstants.RETURN_PARAM_NAME, codingGapReportBundle));
		}

		return result;
	}

	@Override
	public void validateParameters(RequestDetails requestDetails) {
		Operations.validateCardinality(requestDetails, RAConstants.PERIOD_START, 1);
		Operations.validateCardinality(requestDetails, RAConstants.PERIOD_END, 1);
		Operations.validateCardinality(requestDetails, RAConstants.SUBJECT, 1);
		Operations.validatePeriod(requestDetails, RAConstants.PERIOD_START, RAConstants.PERIOD_END);
		Operations.validateSingularPattern(requestDetails, RAConstants.SUBJECT,
				Operations.PATIENT_OR_GROUP_REFERENCE);
		Operations.validateAtLeastOne(requestDetails, RAConstants.MEASURE_ID,
				RAConstants.MEASURE_IDENTIFIER, RAConstants.MEASURE_URL);
	}
}
