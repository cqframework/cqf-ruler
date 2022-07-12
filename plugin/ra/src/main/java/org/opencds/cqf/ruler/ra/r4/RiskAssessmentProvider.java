package org.opencds.cqf.ruler.ra.r4;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Quantity;
import org.opencds.cqf.ruler.cr.r4.provider.MeasureEvaluateProvider;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

public class RiskAssessmentProvider extends DaoRegistryOperationProvider {
	@Autowired
	MeasureEvaluateProvider measureEvaluateProvider;

	private String suspectTypeUrl = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-suspectType";
	private Extension historicExtension = new Extension().setUrl(suspectTypeUrl).setValue(
		new CodeableConcept().setCoding(
			Collections.singletonList(new Coding().setCode("historic").setSystem(suspectTypeUrl))
		)
	);
	private Extension suspectedExtension = new Extension().setUrl(suspectTypeUrl).setValue(
		new CodeableConcept().setCoding(
			Collections.singletonList(new Coding().setCode("suspected").setSystem(suspectTypeUrl))
		)
	);
	private Extension netNewExtension = new Extension().setUrl(suspectTypeUrl).setValue(
		new CodeableConcept().setCoding(
			Collections.singletonList(new Coding().setCode("net-new").setSystem(suspectTypeUrl))
		)
	);

	private String evidenceStatusUrl = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-evidenceStatus";
	private Extension closedGapExtension = new Extension().setUrl(evidenceStatusUrl).setValue(
		new CodeableConcept().setCoding(
			Collections.singletonList(new Coding().setCode("closed-gap").setSystem(evidenceStatusUrl))
		)
	);
	private Extension openGapExtension = new Extension().setUrl(evidenceStatusUrl).setValue(
		new CodeableConcept().setCoding(
			Collections.singletonList(new Coding().setCode("open-gap").setSystem(evidenceStatusUrl))
		)
	);

	private String evidenceStatusDateUrl = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-evidenceStatus";

	private String visited;

	@Operation(name = "$risk-assessment", idempotent = true, type = Measure.class)
	public MeasureReport riskAssessment(
		RequestDetails requestDetails, @IdParam IdType theId,
		@OperationParam(name = "periodStart") String periodStart,
		@OperationParam(name = "periodEnd") String periodEnd,
		@OperationParam(name = "reportType") String reportType,
		@OperationParam(name = "subject") String subject,
		@OperationParam(name = "practitioner") String practitioner,
		@OperationParam(name = "lastReceivedOn") String lastReceivedOn,
		@OperationParam(name = "productLine") String productLine,
		@OperationParam(name = "additionalData") Bundle additionalData,
		@OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint)
	{
		MeasureReport evaluateResult = measureEvaluateProvider.evaluateMeasure(
			requestDetails, theId, periodStart, periodEnd, reportType, subject, practitioner,
			lastReceivedOn, productLine, additionalData, terminologyEndpoint
		);

		/*
		*
		* Historic Gap Closed = Measure Score 1, Historic Stratifier true
		* Historic Gap Open = Measure Score 0, Historic Stratifier true
		* Net New = Measure Score 1, Net-New Stratifier true
		* Suspected Gap Closed = Measure Score 1, Suspected Stratifier true
		* Suspected Gap Open = Measure Score 0, Suspected Stratifier true
		*
		* */

		MeasureReport riskAssessmentResult = new MeasureReport();
		evaluateResult.copyValues(riskAssessmentResult);
		riskAssessmentResult.getGroup().clear();

		for (MeasureReport.MeasureReportGroupComponent group : evaluateResult.getGroup()) {
			CodeableConcept hccCode = group.getCode();
			visited = null;
			for (MeasureReport.MeasureReportGroupStratifierComponent stratifier : group.getStratifier()) {
				CodeableConcept stratifierPopCode = stratifier.getCodeFirstRep();
				for (MeasureReport.StratifierGroupComponent stratum : stratifier.getStratum()) {
					CodeableConcept value = stratum.getValue();
					Quantity score = stratum.getMeasureScore();
					if (stratifierPopCode.hasCoding() && stratifierPopCode.getCodingFirstRep().getCode().equals("historic")) {
						riskAssessmentResult.addGroup(resolveGroup(hccCode, value, score, historicExtension));
					}
					else if (stratifierPopCode.hasCoding() && stratifierPopCode.getCodingFirstRep().getCode().equals("suspected")) {
						riskAssessmentResult.addGroup(resolveGroup(hccCode, value, score, suspectedExtension));
					}
					else if (stratifierPopCode.hasCoding() && stratifierPopCode.getCodingFirstRep().getCode().equals("net-new")) {
						riskAssessmentResult.addGroup(resolveGroup(hccCode, value, score, netNewExtension));
					}
				}
			}
		}

		return riskAssessmentResult;
	}

	private MeasureReport.MeasureReportGroupComponent resolveGroup(CodeableConcept hccCode, CodeableConcept value, Quantity score, Extension extension) {
		MeasureReport.MeasureReportGroupComponent group = new MeasureReport.MeasureReportGroupComponent();
		if (value != null && value.hasText() && value.getText().equalsIgnoreCase("true")) {
			if (visited != null) {
				throw new RuntimeException(String.format(
					"Disjoint populations found. The %s and %s populations are cannot be included in the same group",
					visited, ((CodeableConcept) extension.getValue()).getCodingFirstRep().getCode())
				);
			}
			if (((CodeableConcept) extension.getValue()).getCodingFirstRep().getCode().equalsIgnoreCase("net-new")
				&& score.hasValue() && score.getValue().intValue() == 0) {
				throw new RuntimeException("Invalid open gap detected for net-new population");
			}
			group
				.setCode(hccCode)
				.addExtension(extension).addExtension(score.hasValue() && score.getValue().intValue() == 1 ? closedGapExtension : openGapExtension);
			visited = ((CodeableConcept) extension.getValue()).getCodingFirstRep().getCode();
		}
		return group.isEmpty() ? null : group;
	}

	private Extension resolveEvidenceStatusDate() {
		// TODO
		return new Extension();
	}

}
