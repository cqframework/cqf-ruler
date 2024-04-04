package org.opencds.cqf.ruler.ra.r4;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;

import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.behavior.r4.MeasureReportUser;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.ra.RAConstants;
import org.opencds.cqf.ruler.utility.Operations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

@Configurable
public class RiskAdjustmentProvider extends DaoRegistryOperationProvider implements MeasureReportUser {

	@Autowired
	ca.uhn.fhir.cr.r4.measure.MeasureOperationsProvider measureEvaluateProvider;

	private String visited;

	@Operation(name = "$davinci-ra.evaluate-measure", idempotent = true, type = Measure.class)
	public Parameters evaluateRiskConditionCategory(
			RequestDetails requestDetails,
			@IdParam IdType theId,
			@OperationParam(name = RAConstants.PERIOD_START, typeName = "date") IPrimitiveType<Date> periodStart,
			@OperationParam(name = RAConstants.PERIOD_END, typeName = "date") IPrimitiveType<Date> periodEnd,
			@OperationParam(name = RAConstants.SUBJECT) String subject) {

		try {
			Operations.validateCardinality(requestDetails, RAConstants.PERIOD_START, 1);
			Operations.validateCardinality(requestDetails, RAConstants.PERIOD_END, 1);
			Operations.validateCardinality(requestDetails, RAConstants.SUBJECT, 1);
		} catch (Exception e) {
			return parameters(part(RAConstants.INVALID_PARAMETERS_NAME,
					generateIssue(RAConstants.INVALID_PARAMETERS_SEVERITY, e.getMessage())));
		}

		ensureSupplementalDataElementSearchParameter(requestDetails);
		// measure report
		MeasureReport unprocessedReport = measureEvaluateProvider.evaluateMeasure(
				theId, periodStart.getValueAsString(), periodEnd.getValueAsString(), null, subject,
				null, null, null, null, null, null, requestDetails);

		Parameters riskAdjustmentParameters = new Parameters();

		RiskAdjustmentReturnElement riskAdjustmentReturnElement = new RiskAdjustmentReturnElement(
				unprocessedReport.getSubject().getReference(),
			unprocessedReport);

		resolveRiskAdjustmentReport(riskAdjustmentReturnElement);

		riskAdjustmentParameters.addParameter()
				.setName(riskAdjustmentReturnElement.reference)
				.setResource(riskAdjustmentReturnElement.getRiskAdjustmentOutcome());

		return riskAdjustmentParameters;
	}

	private void resolveRiskAdjustmentReport(RiskAdjustmentReturnElement riskAdjustmentReturnElement) {


		for (MeasureReport.MeasureReportGroupComponent group : riskAdjustmentReturnElement.unprocessedReport.getGroup()) {
			CodeableConcept hccCode = group.getCode();
			visited = null;
			for (MeasureReport.MeasureReportGroupStratifierComponent stratifier : group.getStratifier()) {
				CodeableConcept stratifierPopCode = stratifier.getCodeFirstRep();

				for (MeasureReport.StratifierGroupComponent stratum : stratifier.getStratum()) {
					CodeableConcept value = stratum.getValue();
					Quantity score = stratum.getMeasureScore();

					if (stratifierPopCode.hasCoding() && stratifierPopCode.getCodingFirstRep().getCode().equals(RAConstants.HISTORIC_CODE)) {

						resolveGroup(riskAdjustmentReturnElement, new Historic(hccCode, value, score, resolveEvidenceStatusDate(riskAdjustmentReturnElement)));

					} else if (stratifierPopCode.hasCoding() && stratifierPopCode.getCodingFirstRep().getCode().equals(RAConstants.SUSPECTED_CODE)) {

						resolveGroup(riskAdjustmentReturnElement, new Suspected(hccCode, value, score, resolveEvidenceStatusDate(riskAdjustmentReturnElement)));

					} else if (stratifierPopCode.hasCoding() && stratifierPopCode.getCodingFirstRep().getCode().equals(RAConstants.NET_NEW_CODE)) {

						resolveGroup(riskAdjustmentReturnElement, new NetNew(hccCode, value, score, resolveEvidenceStatusDate(riskAdjustmentReturnElement)));
					}
				}
			}
		}
	}

	private void resolveGroup(RiskAdjustmentReturnElement riskAdjustmentReturnElement,
			RiskAdjustmentGroup riskAdjustmentGroup) {
		if (riskAdjustmentGroup.value != null && riskAdjustmentGroup.value.hasText()
				&& riskAdjustmentGroup.value.getText().equalsIgnoreCase("true")) {
			if (visited != null) {
				riskAdjustmentReturnElement.createIssue(
						String.format(
								"Disjoint populations found. The %s and %s populations cannot be included in the same group",
								visited, riskAdjustmentGroup.name));
			} else if (riskAdjustmentGroup instanceof NetNew && riskAdjustmentGroup.score.hasValue()
					&& riskAdjustmentGroup.score.getValue().intValue() == 0) {
				riskAdjustmentReturnElement.createIssue("Invalid open gap detected for net-new population");
			} else {
				riskAdjustmentReturnElement.processedReport.addGroup(riskAdjustmentGroup.resolveGroup());
				visited = riskAdjustmentGroup.name;
			}
		}
	}

	private Extension resolveEvidenceStatusDate(RiskAdjustmentReturnElement riskAdjustmentReturnElement) {
		for (Resource contained : riskAdjustmentReturnElement.unprocessedReport.getContained()) {
			if (!(contained instanceof Observation))
				continue;
			Observation containedObservation = (Observation) contained;
			if (containedObservation.hasCode() && containedObservation.getCode().hasCoding()
					&& containedObservation.getCode().getCodingFirstRep().hasSystem()
					&& containedObservation.getCode().getCodingFirstRep().getSystem().equals(
							"http://terminology.hl7.org/CodeSystem/measure-data-usage")
					&& ((Observation) contained).hasValueCodeableConcept()
					&& ((Observation) contained).getValueCodeableConcept().hasCoding()
					&& ((Observation) contained).getValueCodeableConcept().getCodingFirstRep().hasCode()) {
				return new Extension().setUrl(RAConstants.EVIDENCE_STATUS_DATE_URL).setValue(
						new DateType(
								((Observation) contained).getValueCodeableConcept().getCodingFirstRep().getCode()));
			}
		}
		return null;
	}

	private static class RiskAdjustmentGroup {

		String name;
		CodeableConcept hccCode;
		CodeableConcept value;
		Quantity score;
		Extension suspectTypeExtension;
		Extension evidenceStatusExtension;
		Extension evidenceStatusDateExtension;

		RiskAdjustmentGroup(CodeableConcept hccCode, CodeableConcept value, Quantity score,
				Extension evidenceStatusDateExtension) {
			this.hccCode = hccCode;
			this.value = value;
			this.score = score;
			this.evidenceStatusDateExtension = evidenceStatusDateExtension;
		}

		MeasureReport.MeasureReportGroupComponent resolveGroup() {
			MeasureReport.MeasureReportGroupComponent group = new MeasureReport.MeasureReportGroupComponent();
			evidenceStatusExtension = score.hasValue() && score.getValue().intValue() == 1
					? RAConstants.EVIDENCE_STATUS_CLOSED_EXT
					: RAConstants.EVIDENCE_STATUS_OPEN_EXT;
			group.setCode(hccCode)
					.addExtension(suspectTypeExtension)
					.addExtension(evidenceStatusExtension)
					.addExtension(evidenceStatusDateExtension);
			return group;
		}
	}

	private static class Historic extends RiskAdjustmentGroup {

		Historic(CodeableConcept hccCode, CodeableConcept value, Quantity score,
				Extension evidenceStatusDateExtension) {
			super(hccCode, value, score, evidenceStatusDateExtension);
			this.suspectTypeExtension = RAConstants.SUSPECT_TYPE_HISTORIC_EXT;
			this.name = RAConstants.HISTORIC_CODE;
		}
	}

	private static class Suspected extends RiskAdjustmentGroup {

		Suspected(CodeableConcept hccCode, CodeableConcept value, Quantity score,
				Extension evidenceStatusDateExtension) {
			super(hccCode, value, score, evidenceStatusDateExtension);
			this.suspectTypeExtension = RAConstants.SUSPECT_TYPE_SUSPECTED_EXT;
			this.name = RAConstants.SUSPECTED_CODE;
		}
	}

	private static class NetNew extends RiskAdjustmentGroup {

		NetNew(CodeableConcept hccCode, CodeableConcept value, Quantity score, Extension evidenceStatusDateExtension) {
			super(hccCode, value, score, evidenceStatusDateExtension);
			this.suspectTypeExtension = RAConstants.SUSPECT_TYPE_NET_NEW_EXT;
			this.name = RAConstants.NET_NEW_CODE;
		}
	}

	private class RiskAdjustmentReturnElement {
		String reference;
		MeasureReport unprocessedReport;
		MeasureReport processedReport;
		OperationOutcome error;

		RiskAdjustmentReturnElement(String reference, MeasureReport unprocessedReport) {
			this.reference = reference;
			this.unprocessedReport = unprocessedReport;
			this.processedReport = new MeasureReport();
			this.unprocessedReport.copyValues(this.processedReport);
			//this.processedReport.getGroup().clear();
			this.processedReport.setMeta(
					new Meta().addProfile(RAConstants.PATIENT_REPORT_URL));
		}

		void createIssue(String issue) {
			this.error = generateIssue(RAConstants.INVALID_PARAMETERS_SEVERITY, issue);
		}

		Resource getRiskAdjustmentOutcome() {
			return this.error == null ? bundleReport() : this.error;
		}

		private Bundle bundleReport() {
			Bundle raBundle = new Bundle().setType(Bundle.BundleType.COLLECTION);
			raBundle.setMeta(new Meta().addProfile(RAConstants.CODING_GAP_BUNDLE_URL));
			raBundle.addEntry().setResource(processedReport);

			Map<String, Resource> evalPlusSDE = new HashMap<>();
			getEvaluatedResources(processedReport, evalPlusSDE).getSDE(processedReport, evalPlusSDE);
			for (Map.Entry<String, Resource> evaluatedResources : evalPlusSDE.entrySet()) {
				raBundle.addEntry().setResource(evaluatedResources.getValue());
			}
			return raBundle;
		}
	}
}
