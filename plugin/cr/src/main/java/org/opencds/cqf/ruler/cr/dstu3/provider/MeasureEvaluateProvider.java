package org.opencds.cqf.ruler.cr.dstu3.provider;

import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.StringType;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.ruler.cql.JpaDataProviderFactory;
import org.opencds.cqf.ruler.cql.JpaFhirDalFactory;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProviderFactory;
import org.opencds.cqf.ruler.cql.JpaTerminologyProviderFactory;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class MeasureEvaluateProvider extends DaoRegistryOperationProvider {
	@Autowired
	private JpaTerminologyProviderFactory jpaTerminologyProviderFactory;

	@Autowired
	private JpaDataProviderFactory dataProviderFactory;

	@Autowired
	private JpaLibraryContentProviderFactory libraryContentProviderFactory;

	@Autowired
	private JpaFhirDalFactory fhirDalFactory;

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
			@OperationParam(name = "productLine") String productLine) {

		Measure measure = read(theId);
		TerminologyProvider terminologyProvider = this.jpaTerminologyProviderFactory.create(requestDetails);
		DataProvider dataProvider = this.dataProviderFactory.create(requestDetails, terminologyProvider);
		LibraryContentProvider libraryContentProvider = this.libraryContentProviderFactory.create(requestDetails);
		FhirDal fhirDal = this.fhirDalFactory.create(requestDetails);

		org.opencds.cqf.cql.evaluator.measure.dstu3.Dstu3MeasureProcessor measureProcessor = new org.opencds.cqf.cql.evaluator.measure.dstu3.Dstu3MeasureProcessor(
				null, null, null, null, null, terminologyProvider, libraryContentProvider, dataProvider, fhirDal, null,
				null);

		MeasureReport report = measureProcessor.evaluateMeasure(measure.getUrl(), periodStart, periodEnd, reportType,
				patient, null, lastReceivedOn, null, null, null, null);

		if (productLine != null) {
			Extension ext = new Extension();
			ext.setUrl("http://hl7.org/fhir/us/cqframework/cqfmeasures/StructureDefinition/cqfm-productLine");
			ext.setValue(new StringType(productLine));
			report.addExtension(ext);
		}

		return report;
	}

}
