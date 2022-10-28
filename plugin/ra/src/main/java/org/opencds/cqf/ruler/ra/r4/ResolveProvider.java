package org.opencds.cqf.ruler.ra.r4;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DetectedIssue;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.ruler.behavior.r4.ParameterUser;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.ra.RAConstants;
import org.opencds.cqf.ruler.utility.Operations;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.opencds.cqf.ruler.utility.r4.Parameters.parameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.part;

public class ResolveProvider extends DaoRegistryOperationProvider implements RiskAdjustmentUser, ParameterUser {

	@Operation(name = "$davinci-ra.resolve", idempotent = true, type = MeasureReport.class)
	public Parameters resolve(
		RequestDetails requestDetails,
		@OperationParam(name = RAConstants.PERIOD_START, typeName = "date") IPrimitiveType<Date> periodStart,
		@OperationParam(name = RAConstants.PERIOD_END, typeName = "date") IPrimitiveType<Date> periodEnd,
		@OperationParam(name = RAConstants.SUBJECT) String subject,
		@OperationParam(name = RAConstants.MEASURE_ID) List<String> measureId,
		@OperationParam(name = RAConstants.MEASURE_IDENTIFIER) List<String> measureIdentifier,
		@OperationParam(name = RAConstants.MEASURE_URL) List<String> measureUrl) {

		try {
			validateParameters(requestDetails);
		} catch (Exception e) {
			return parameters(part(RAConstants.INVALID_PARAMETERS_NAME,
				generateIssue(RAConstants.INVALID_PARAMETERS_SEVERITY, e.getMessage())));
		}

		List<MeasureReport> reports = new ArrayList<>();
		List<String> measureReferences = getMeasureReferences(measureId, measureIdentifier, measureUrl);
		getPatientListFromSubject(subject).forEach(
			patient -> reports.addAll(
				getMeasureReportsWithMeasureReference(
					patient.getIdElement().getIdPart(), periodStart.getValueAsString(),
					periodEnd.getValueAsString(), measureReferences))
		);
		Map<MeasureReport, List<DetectedIssue>> issues = getIssueMap(reports);
		List<Bundle> raBundles = buildCompositionsAndBundles(subject, issues);

		Parameters result = new Parameters();
		for (Bundle raBundle : raBundles) {
			result.addParameter(part(RAConstants.RETURN_PARAM_NAME, raBundle));
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
