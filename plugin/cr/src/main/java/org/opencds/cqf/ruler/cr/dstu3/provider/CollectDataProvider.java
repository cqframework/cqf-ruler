package org.opencds.cqf.ruler.cr.dstu3.provider;

import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newPart;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Resource;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class CollectDataProvider extends DaoRegistryOperationProvider {

	@Autowired
	private MeasureEvaluateProvider measureEvaluateProvider;

	/**
	* Implements the <a href="http://hl7.org/fhir/DSTU3/measure-operation-collect-data.html">$collect-data</a>
	* operation found in the
	* <a href="http://hl7.org/fhir/R4/clinicalreasoning-module.html">FHIR Clinical Reasoning Module</a>.
	* 
	* <p>Returns a set of parameters with the generated MeasureReport and the
	* resources that were used during the Measure evaluation
	* 
	* @param theRequestDetails generally auto-populated by the HAPI server framework.
	* @param theId             the Id of the Measure to sub data for
	* @param periodStart       The start of the reporting period
	* @param periodEnd         The end of the reporting period
	* @param subject           the subject to use for the evaluation
	* @param practitioner      the practitioner to use for the evaluation
	* @param lastReceivedOn    the date the results of this measure were last received.
	* @return Parameters the parameters containing the MeasureReport and the evaluated Resources
	*/
	@Description(shortDefinition = "$collect-data", value = "Implements the <a href=\"http://hl7.org/fhir/R4/measure-operation-collect-data.html\">$collect-data</a> operation found in the <a href=\"http://hl7.org/fhir/R4/clinicalreasoning-module.html\">FHIR Clinical Reasoning Module</a>.")
	@Operation(name = "$collect-data", idempotent = true, type = Measure.class)
	public Parameters collectData(RequestDetails theRequestDetails, @IdParam IdType theId,
			@OperationParam(name = "periodStart") String periodStart,
			@OperationParam(name = "periodEnd") String periodEnd, @OperationParam(name = "subject") String subject,
			@OperationParam(name = "practitioner") String practitioner,
			@OperationParam(name = "lastReceivedOn") String lastReceivedOn) {

		MeasureReport report = measureEvaluateProvider.evaluateMeasure(theRequestDetails, theId, periodStart, periodEnd,
				"subject", subject, practitioner, lastReceivedOn, null);

		// TODO: Data collection doesn't exist?
		report.setType(MeasureReportType.SUMMARY);
		report.setGroup(null);

		Parameters parameters = newParameters(newPart("measureReport", report));

		addEvaluatedResourcesToParameters(report, parameters);

		return parameters;
	}

	private List<Resource> readEvaluatedResources(MeasureReport report) {
		List<Resource> resources = new ArrayList<>();
		for (BundleEntryComponent entry : report.getEvaluatedResourcesTarget().getEntry()) {
			Resource resource = entry.getResource();
			if (resource != null) {
				resources.add(resource);
			}
		}

		return resources;
	}

	private void addEvaluatedResourcesToParameters(MeasureReport report, Parameters parameters) {
		readEvaluatedResources(report)
				.forEach(resource -> parameters.addParameter(newPart("resource", resource)));
	}
}
