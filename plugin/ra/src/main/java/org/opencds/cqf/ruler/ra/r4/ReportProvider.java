package org.opencds.cqf.ruler.ra.r4;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.behavior.ResourceCreator;
import org.opencds.cqf.ruler.behavior.r4.MeasureReportUser;
import org.opencds.cqf.ruler.behavior.r4.ParameterUser;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.ra.RAConstants;
import org.opencds.cqf.ruler.utility.Operations;
import org.opencds.cqf.ruler.utility.Searches;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;

import static org.opencds.cqf.ruler.utility.r4.Parameters.parameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.part;

public class ReportProvider extends DaoRegistryOperationProvider
	implements ParameterUser, ResourceCreator, MeasureReportUser {

	/**
	 * Implements the <a href=
	 * "https://build.fhir.org/ig/HL7/davinci-ra/OperationDefinition-report.html">$davinci-ra.report</a>
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
	@Description(shortDefinition = "$davinci-ra.report operation",
		value = "Implements the <a href=\"https://build.fhir.org/ig/HL7/davinci-ra/OperationDefinition-davinci-ra.report.html\">$davinci-ra.report</a> operation found in the <a href=\"https://build.fhir.org/ig/HL7/davinci-ra/index.html\">Da Vinci Risk Adjustment IG</a>.")
	@Operation(name = "$davinci-ra.report", idempotent = true, type = MeasureReport.class)
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
		Period period = new Period().setStart(periodStart.getValue()).setEnd(periodEnd.getValue());
		List<Patient> patients = getPatientListFromSubject(subject);

		(patients)
			.forEach(
				patient -> {
					Parameters.ParametersParameterComponent patientParameter = patientReport(patient, period,
						requestDetails.getFhirServerBase());
					result.addParameter(patientParameter);
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

	private Parameters.ParametersParameterComponent patientReport(Patient thePatient, Period thePeriod,
																					  String serverBase) {
		String patientId = thePatient.getIdElement().getIdPart();
		final Map<IIdType, IAnyResource> bundleEntries = new HashMap<>();
		bundleEntries.put(thePatient.getIdElement(), thePatient);

		ReferenceParam subjectParam = new ReferenceParam(patientId);
		search(MeasureReport.class, Searches.byParam(RAConstants.SUBJECT, subjectParam)).getAllResourcesTyped()
			.forEach(measureReport -> {
				if (measureReport.getPeriod().getEnd().before(thePeriod.getStart())
					|| measureReport.getPeriod().getStart().after(thePeriod.getEnd())) {
					return;
				}

				bundleEntries.putIfAbsent(measureReport.getIdElement(), measureReport);

				getEvaluatedResources(measureReport)
					.values()
					.forEach(resource -> bundleEntries.putIfAbsent(resource.getIdElement(), resource));
			});

		Bundle patientReportBundle = new Bundle();
		patientReportBundle.setMeta(new Meta().addProfile(RAConstants.PATIENT_REPORT_PROFILE_URL));
		patientReportBundle.setType(Bundle.BundleType.COLLECTION);
		patientReportBundle.setTimestamp(new Date());
		patientReportBundle.setId(patientId + RAConstants.REPORT_ID_SUFFIX);
		patientReportBundle.setIdentifier(
			new Identifier().setSystem("urn:ietf:rfc:3986").setValue("urn:uuid:" + UUID.randomUUID()));

		bundleEntries.forEach((key, value) -> patientReportBundle.addEntry(
			new Bundle.BundleEntryComponent()
				.setResource((Resource) value)
				.setFullUrl(Operations.getFullUrl(serverBase, value.fhirType(),
					value.getIdElement().getIdPart()))));

		Parameters.ParametersParameterComponent patientParameter = new Parameters.ParametersParameterComponent();
		patientParameter.setResource(patientReportBundle);
		patientParameter.setId(thePatient.getIdElement().getIdPart() + RAConstants.REPORT_ID_SUFFIX);
		patientParameter.setName("return");

		return patientParameter;
	}
}
