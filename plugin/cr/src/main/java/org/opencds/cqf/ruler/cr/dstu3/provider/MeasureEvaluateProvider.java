package org.opencds.cqf.ruler.cr.dstu3.provider;

import java.util.function.Function;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.cr.dstu3.service.MeasureService;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class MeasureEvaluateProvider implements OperationProvider {

	@Autowired
	Function<RequestDetails, MeasureService> dstu3MeasureServiceFactory;

	/**
	 * Implements the <a href=
	 * "https://www.hl7.org/fhir/operation-measure-evaluate-measure.html">$evaluate-measure</a>
	 * operation found in the
	 * <a href="http://www.hl7.org/fhir/clinicalreasoning-module.html">FHIR Clinical
	 * Reasoning Module</a>. This implementation aims to be compatible with the CQF
	 * IG.
	 *
	 * @param requestDetails The details (such as tenant) of this request. Usually
	 *                       auto-populated HAPI.
	 * @param theId          the Id of the Measure to evaluate
	 * @param periodStart    The start of the reporting period
	 * @param periodEnd      The end of the reporting period
	 * @param reportType     The type of MeasureReport to generate
	 * @param patient        the patient to use as the subject to use for the
	 *                       evaluation
	 * @param practitioner   the practitioner to use for the evaluation
	 * @param lastReceivedOn the date the results of this measure were last
	 *                       received.
	 * @param productLine    the productLine (e.g. Medicare, Medicaid, etc) to use
	 *                       for the evaluation. This is a non-standard parameter.
	 * @param additionalData the data bundle containing additional data
	 * @return the calculated MeasureReport
	 */
	@Description(shortDefinition = "$evaluate-measure", value = "Implements the <a href=\"https://www.hl7.org/fhir/operation-measure-evaluate-measure.html\">$evaluate-measure</a> operation found in the <a href=\"http://www.hl7.org/fhir/clinicalreasoning-module.html\">FHIR Clinical Reasoning Module</a>. This implementation aims to be compatible with the CQF IG.", example = "Measure/example/$evaluate-measure?subject=Patient/123&periodStart=2019&periodEnd=2020")
	@Operation(name = "$evaluate-measure", idempotent = true, type = Measure.class)
	public MeasureReport evaluateMeasure(RequestDetails requestDetails, @IdParam IdType theId,
			@OperationParam(name = "periodStart") String periodStart,
			@OperationParam(name = "periodEnd") String periodEnd,
			@OperationParam(name = "reportType") String reportType,
			@OperationParam(name = "patient") String patient,
			@OperationParam(name = "practitioner") String practitioner,
			@OperationParam(name = "lastReceivedOn") String lastReceivedOn,
			@OperationParam(name = "productLine") String productLine,
			@OperationParam(name = "additionalData") Bundle additionalData,
			@OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint) {

		return this.dstu3MeasureServiceFactory
				.apply(requestDetails)
				.evaluateMeasure(
						theId,
						periodStart,
						periodEnd,
						reportType,
						patient,
						practitioner,
						lastReceivedOn,
						productLine,
						additionalData,
						terminologyEndpoint);
	}

}
