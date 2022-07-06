package org.opencds.cqf.ruler.ra.r4;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.ruler.cpg.r4.provider.CqlExecutionProvider;
import org.opencds.cqf.ruler.cr.r4.provider.MeasureEvaluateProvider;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;

public class RiskAssessmentProvider extends DaoRegistryOperationProvider {

	@Operation(name = "$risk-assessment")
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

		MeasureEvaluateProvider provider = new MeasureEvaluateProvider();
		MeasureReport evaluateResult = provider.evaluateMeasure(
			requestDetails, theId, periodStart, periodEnd, reportType, subject, practitioner,
			lastReceivedOn, productLine, additionalData, terminologyEndpoint
		);

		MeasureReport riskAssessmentResult = new MeasureReport();

		Measure evaluatedMeasure = read(theId);
		Library primaryLibrary;
		if (evaluatedMeasure.hasLibrary()) {
			primaryLibrary = read(new IdType().setValue(evaluatedMeasure.getLibrary().get(0).getId()));
		}
		else throw new RuntimeException("The Measure/$risk-assessment operation requires the specified measure to have a reference to a primary library");

		String content = null;
		if (primaryLibrary.hasContent()) {
			for (Attachment a : primaryLibrary.getContent()) {
				if (a.hasContentType() && a.getContentType().equals("text/cql")) {
					content = new String(a.getData());
				}
			}
		}
		if (StringUtils.isBlank(content)) throw new RuntimeException("The Measure/$risk-assessment operation requires the primary library to include cql content");

		CqlExecutionProvider cqlExecutionProvider = new CqlExecutionProvider();
//		Parameters conditionCategoryResult = cqlExecutionProvider.evaluate(
//			requestDetails, subject, "Condition Category", null, null, null,
//			null, null, null, null, terminologyEndpoint, content
//		);

		// TODO: Add MeasureReport group extension for HCC code based on result returned from the $cql operation

		return riskAssessmentResult;
	}

}
