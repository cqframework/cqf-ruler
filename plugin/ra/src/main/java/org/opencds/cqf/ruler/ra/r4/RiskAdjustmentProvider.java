package org.opencds.cqf.ruler.ra.r4;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
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
import org.opencds.cqf.ruler.cr.r4.provider.MeasureEvaluateProvider;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.Operations;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class RiskAdjustmentProvider extends DaoRegistryOperationProvider implements MeasureReportUser {

	@Autowired
	MeasureEvaluateProvider measureEvaluateProvider;

	private static String suspectTypeUrl = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-suspectType";
	private static String evidenceStatusUrl = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-evidenceStatus";
	private static String evidenceStatusDateUrl = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-evidenceStatusDate";

	private String visited;

	@Operation(name = "$evaluate-risk-condition-category", idempotent = true, type = Measure.class)
	public Parameters evaluateRiskConditionCategory(
			RequestDetails requestDetails,
			@IdParam IdType theId,
			@OperationParam(name = "type") String type,
			@OperationParam(name = "periodStart") String periodStart,
			@OperationParam(name = "periodEnd") String periodEnd,
			@OperationParam(name = "subject") String subject) {

		if (requestDetails.getRequestType() == RequestTypeEnum.GET) {
			try {
				Operations.validateCardinality(requestDetails, "type", 1);
				Operations.validateCardinality(requestDetails, "periodStart", 1);
				Operations.validateCardinality(requestDetails, "periodEnd", 1);
				Operations.validateCardinality(requestDetails, "subject", 1);
			} catch (Exception e) {
				return org.opencds.cqf.ruler.utility.r4.Parameters.newParameters(
						org.opencds.cqf.ruler.utility.r4.Parameters.newPart("Invalid parameters",
								generateIssue("error", e.getMessage())));
			}
		}

		if (!type.equalsIgnoreCase("report")) {
			org.opencds.cqf.ruler.utility.r4.Parameters.newParameters(
					org.opencds.cqf.ruler.utility.r4.Parameters.newPart(
							subject,
							generateIssue("error", String.format(
									"The $risk-adjustment operation is not implemented for %s type parameter on this server",
									type))));
		}

		ensureSupplementalDataElementSearchParameter(requestDetails);

		MeasureReport unprocessedReport = measureEvaluateProvider.evaluateMeasure(
				requestDetails, theId, periodStart, periodEnd, null, subject, null,
				null, null, null, null);

		Parameters riskAdjustmentParameters = new Parameters();

		RiskAdjustmentReturnElement riskAdjustmentReturnElement = new RiskAdjustmentReturnElement(
				unprocessedReport.getSubject().getReference(), unprocessedReport);
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
					if (stratifierPopCode.hasCoding()
							&& stratifierPopCode.getCodingFirstRep().getCode().equals("historic")) {
						resolveGroup(riskAdjustmentReturnElement,
								new Historic(hccCode, value, score, resolveEvidenceStatusDate(riskAdjustmentReturnElement)));
					} else if (stratifierPopCode.hasCoding()
							&& stratifierPopCode.getCodingFirstRep().getCode().equals("suspected")) {
						resolveGroup(riskAdjustmentReturnElement,
								new Suspected(hccCode, value, score, resolveEvidenceStatusDate(riskAdjustmentReturnElement)));
					} else if (stratifierPopCode.hasCoding()
							&& stratifierPopCode.getCodingFirstRep().getCode().equals("net-new")) {
						resolveGroup(riskAdjustmentReturnElement,
								new NetNew(hccCode, value, score, resolveEvidenceStatusDate(riskAdjustmentReturnElement)));
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
			if (!(contained instanceof Observation)) continue;
			Observation containedObservation = (Observation) contained;
			if (containedObservation.hasCode() && containedObservation.getCode().hasCoding()
					&& containedObservation.getCode().getCodingFirstRep().hasSystem()
					&& containedObservation.getCode().getCodingFirstRep().getSystem().equals(
							"http://terminology.hl7.org/CodeSystem/measure-data-usage")
					&& ((Observation) contained).hasValueCodeableConcept()
					&& ((Observation) contained).getValueCodeableConcept().hasCoding()
					&& ((Observation) contained).getValueCodeableConcept().getCodingFirstRep().hasCode()) {
				return new Extension().setUrl(evidenceStatusDateUrl).setValue(
						new CodeableConcept().setCoding(
								Collections.singletonList(new Coding()
										.setCode(
												((Observation) contained).getValueCodeableConcept().getCodingFirstRep().getCode())
										.setSystem(evidenceStatusDateUrl))));
			}
		}
		return null;
	}

	private static class RiskAdjustmentGroup {
		private Extension closedGapExtension = new Extension().setUrl(evidenceStatusUrl).setValue(
				new CodeableConcept().setCoding(
						Collections.singletonList(new Coding().setCode("closed-gap").setSystem(evidenceStatusUrl))));
		private Extension openGapExtension = new Extension().setUrl(evidenceStatusUrl).setValue(
				new CodeableConcept().setCoding(
						Collections.singletonList(new Coding().setCode("open-gap").setSystem(evidenceStatusUrl))));

		String name;
		CodeableConcept hccCode;
		CodeableConcept value;
		Quantity score;
		Extension supectTypeExtension;
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
			evidenceStatusExtension = score.hasValue() && score.getValue().intValue() == 1 ? closedGapExtension
					: openGapExtension;
			group.setCode(hccCode)
					.addExtension(supectTypeExtension)
					.addExtension(evidenceStatusExtension)
					.addExtension(evidenceStatusDateExtension);
			return group;
		}
	}

	private static class Historic extends RiskAdjustmentGroup {

		Historic(CodeableConcept hccCode, CodeableConcept value, Quantity score, Extension evidenceStatusDateExtension) {
			super(hccCode, value, score, evidenceStatusDateExtension);
			this.supectTypeExtension = new Extension().setUrl(suspectTypeUrl).setValue(
					new CodeableConcept().setCoding(
							Collections.singletonList(new Coding().setCode("historic").setSystem(suspectTypeUrl))));
			this.name = "historic";
		}
	}

	private static class Suspected extends RiskAdjustmentGroup {

		Suspected(CodeableConcept hccCode, CodeableConcept value, Quantity score, Extension evidenceStatusDateExtension) {
			super(hccCode, value, score, evidenceStatusDateExtension);
			this.supectTypeExtension = new Extension().setUrl(suspectTypeUrl).setValue(
					new CodeableConcept().setCoding(
							Collections.singletonList(new Coding().setCode("suspected").setSystem(suspectTypeUrl))));
			this.name = "suspected";
		}
	}

	private static class NetNew extends RiskAdjustmentGroup {

		NetNew(CodeableConcept hccCode, CodeableConcept value, Quantity score, Extension evidenceStatusDateExtension) {
			super(hccCode, value, score, evidenceStatusDateExtension);
			this.supectTypeExtension = new Extension().setUrl(suspectTypeUrl).setValue(
					new CodeableConcept().setCoding(
							Collections.singletonList(new Coding().setCode("net-new").setSystem(suspectTypeUrl))));
			this.name = "net-new";
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
			this.processedReport.getGroup().clear();
			this.processedReport.setMeta(
					new Meta().addProfile("http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-measurereport"));
		}

		void createIssue(String issue) {
			this.error = generateIssue("error", issue);
		}

		Resource getRiskAdjustmentOutcome() {
			return this.error == null ? bundleReport() : this.error;
		}

		private Bundle bundleReport() {
			Bundle raBundle = new Bundle().setType(Bundle.BundleType.COLLECTION);
			raBundle.setMeta(
					new Meta().addProfile("http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-measurereport-bundle"));
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
