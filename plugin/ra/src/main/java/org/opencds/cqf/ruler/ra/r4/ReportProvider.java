package org.opencds.cqf.ruler.ra.r4;

import static org.opencds.cqf.ruler.utility.r4.Parameters.parameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.part;

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
import org.opencds.cqf.ruler.behavior.ResourceCreator;
import org.opencds.cqf.ruler.behavior.r4.MeasureReportUser;
import org.opencds.cqf.ruler.behavior.r4.ParameterUser;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.ra.RAConstants;
import org.opencds.cqf.ruler.utility.Operations;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class ReportProvider extends DaoRegistryOperationProvider
		implements ParameterUser, ResourceCreator, MeasureReportUser, RiskAdjustmentUser {

	/**
	 * Implements the <a href=
	 * "https://build.fhir.org/ig/HL7/davinci-ra/OperationDefinition-report.html">$ra.report</a>
	 * operation found in the
	 * <a href="https://build.fhir.org/ig/HL7/davinci-ra/index.html">Da Vinci Risk
	 * Adjustment IG</a>.
	 *
	 * @param requestDetails metadata about the current request being processed.
	 *                       Generally auto-populated by the HAPI FHIR server
	 *                       framework.
	 * @param periodStart    the start of the clinical evaluation period
	 * @param periodEnd      the end of the clinical evaluation period
	 * @param subject        a Patient or Patient Group
	 * @return a Parameters with Bundles of MeasureReports and evaluatedResource
	 *         Resources
	 */
	@Description(shortDefinition = "$ra.report operation", value = "Implements the <a href=\"https://build.fhir.org/ig/HL7/davinci-ra/OperationDefinition-davinci-ra.report.html\">$ra.report</a> operation found in the <a href=\"https://build.fhir.org/ig/HL7/davinci-ra/index.html\">Da Vinci Risk Adjustment IG</a>.")
	@Operation(name = "$ra.report", idempotent = true, type = MeasureReport.class)
	public Parameters report(
			RequestDetails requestDetails,
			@OperationParam(name = RAConstants.PERIOD_START, typeName = "date") IPrimitiveType<Date> periodStart,
			@OperationParam(name = RAConstants.PERIOD_END, typeName = "date") IPrimitiveType<Date> periodEnd,
			@OperationParam(name = RAConstants.SUBJECT) String subject) throws FHIRException {

		try {
			validateParameters(requestDetails);
		} catch (Exception e) {
			return parameters(part(RAConstants.INVALID_PARAMETERS_NAME,
					generateIssue(RAConstants.INVALID_PARAMETERS_SEVERITY, e.getMessage())));
		}

		ensureSupplementalDataElementSearchParameter(requestDetails);

		Parameters result = newResource(Parameters.class,
				subject.replace("/", "-") + RAConstants.REPORT_ID_SUFFIX);

		getPatientListFromSubject(subject).forEach(
				patient -> {
					List<MeasureReport> reports = getMeasureReports(
							patient.getIdElement().getIdPart(), periodStart.getValueAsString(), periodEnd.getValueAsString());
					if (reports.isEmpty()) {
						result.addParameter(part(
								RAConstants.RETURN_PARAM_NAME, buildMissingMeasureReportCodingGapReportBundle(patient)));
					} else {
						reports.forEach(report -> {
							List<DetectedIssue> issues = getOriginalIssues(report.getId());
							Composition composition = buildComposition(subject, report, issues);
							Bundle bundle = buildCodingGapReportBundle(composition, issues, report);
							result.addParameter(part(RAConstants.RETURN_PARAM_NAME,
								bundle.setId("condition-category-report-" + UUID.randomUUID())));
						});
					}
				});

		return result;
	}

	public void validateParameters(RequestDetails requestDetails) {
		Operations.validateCardinality(requestDetails, RAConstants.PERIOD_START, 1);
		Operations.validateCardinality(requestDetails, RAConstants.PERIOD_END, 1);
		Operations.validatePeriod(requestDetails, RAConstants.PERIOD_START, RAConstants.PERIOD_END);
		Operations.validateCardinality(requestDetails, RAConstants.SUBJECT, 1);
		Operations.validateSingularPattern(requestDetails, RAConstants.SUBJECT,
				Operations.PATIENT_OR_GROUP_REFERENCE);
	}
}
