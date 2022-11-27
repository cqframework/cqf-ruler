package org.opencds.cqf.ruler.ra.r4;

import static org.opencds.cqf.ruler.utility.r4.Parameters.parameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.part;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DetectedIssue;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.ruler.behavior.r4.ParameterUser;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.ra.RAConstants;
import org.opencds.cqf.ruler.utility.Operations;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class RemediateProvider extends DaoRegistryOperationProvider implements RiskAdjustmentUser, ParameterUser {

	@Operation(name = "$ra.remediate-coding-gaps", idempotent = true, type = MeasureReport.class)
	public Parameters remediate(
			RequestDetails requestDetails,
			@OperationParam(name = RAConstants.PERIOD_START, typeName = "date") IPrimitiveType<Date> periodStart,
			@OperationParam(name = RAConstants.PERIOD_END, typeName = "date") IPrimitiveType<Date> periodEnd,
			@OperationParam(name = RAConstants.SUBJECT) String subject) {

		try {
			validateParameters(requestDetails);
		} catch (Exception e) {
			return parameters(part(RAConstants.INVALID_PARAMETERS_NAME,
					generateIssue(RAConstants.INVALID_PARAMETERS_SEVERITY, e.getMessage())));
		}

		List<Bundle> codingGapReportBundles = new ArrayList<>();
		getPatientListFromSubject(subject).forEach(
				patient -> {
					Bundle b = getMostRecentCodingGapReportBundle(subject, periodStart.getValue(), periodEnd.getValue());
					MeasureReport mr = getReportFromBundle(b);
					Composition composition = getCompositionFromBundle(b);
					List<DetectedIssue> issues = getIssuesFromBundle(b);
					issues.addAll(getAssociatedIssues(mr.getIdElement().getValue()));
					updateComposition(composition, mr, issues);

					// TODO: get author from bundle
					codingGapReportBundles.add(
							buildCodingGapReportBundle(requestDetails.getFhirServerBase(), composition, issues, mr, null));
				});

		Parameters result = new Parameters();
		result.setId(RAConstants.REMEDIATE_ID_PREFIX + UUID.randomUUID());
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
	}
}
