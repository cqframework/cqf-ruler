package org.opencds.cqf.ruler.ra.r4;

import static com.google.common.base.Preconditions.checkArgument;
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
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.behavior.ConfigurationUser;
import org.opencds.cqf.ruler.behavior.ResourceCreator;
import org.opencds.cqf.ruler.behavior.r4.ParameterUser;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.ra.RAConstants;
import org.opencds.cqf.ruler.ra.RAProperties;
import org.opencds.cqf.ruler.utility.Operations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.google.common.base.Strings;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

@Configurable
public class RACodingGapsProvider extends DaoRegistryOperationProvider
		implements ParameterUser, ConfigurationUser, ResourceCreator, RiskAdjustmentUser {

	@Autowired
	private RAProperties raProperties;

	private IdType compositionSectionAuthor;
	private Resource author;

	/**
	 * Implements the <a href=
	 * "https://build.fhir.org/ig/HL7/davinci-ra/OperationDefinition-ra.coding-gaps.html">$ra.coding-gaps</a>
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
	@Description(shortDefinition = "$ra.coding-gaps operation", value = "Implements the <a href=\"https://build.fhir.org/ig/HL7/davinci-ra/OperationDefinition-ra.coding-gaps.html\">$ra.coding-gaps</a> operation found in the <a href=\"https://build.fhir.org/ig/HL7/davinci-ra/index.html\">Da Vinci Risk Adjustment IG</a>.")
	@Operation(name = "$ra.coding-gaps", idempotent = true, type = MeasureReport.class)
	public Parameters raCodingGaps(
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
		try {
			validateConfiguration(requestDetails);
		} catch (Exception e) {
			return parameters(part("Invalid configuration", generateIssue("error", e.getMessage())));
		}

		ensureSupplementalDataElementSearchParameter(requestDetails);

		Parameters result = newResource(Parameters.class,
				RAConstants.REPORT_ID_PREFIX + UUID.randomUUID());

		getPatientListFromSubject(subject).forEach(
				patient -> {
					List<MeasureReport> reports = getMeasureReports(
							patient.getIdElement().getIdPart(), periodStart.getValueAsString(), periodEnd.getValueAsString());
					if (reports.isEmpty()) {
						result.addParameter(part(
								RAConstants.RETURN_PARAM_NAME,
								buildMissingMeasureReportCodingGapReportBundle(requestDetails.getFhirServerBase(), patient)));
					} else {
						reports.forEach(report -> {
							List<DetectedIssue> issues = buildOriginalIssues(report);
							Composition composition = buildComposition(subject, report, issues, compositionSectionAuthor);
							Bundle bundle = buildCodingGapReportBundle(requestDetails.getFhirServerBase(), composition, issues,
									report, author);
							result.addParameter(part(RAConstants.RETURN_PARAM_NAME,
									bundle.setId(UUID.randomUUID().toString())));
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

	@Override
	public void validateConfiguration(RequestDetails theRequestDetails) {
		checkArgument(
				raProperties.getComposition() != null
						&& !Strings.isNullOrEmpty(raProperties.getComposition().getCompositionSectionAuthor()),
				"The composition.ra_composition_section_author setting is required for the $ra.coding-gaps operation.");
		compositionSectionAuthor = new IdType(raProperties.getComposition().getCompositionSectionAuthor());
		// This will throw a ResourceNotFound exception if the Organization resource is
		// not loaded in the server
		author = read(compositionSectionAuthor);
	}
}
