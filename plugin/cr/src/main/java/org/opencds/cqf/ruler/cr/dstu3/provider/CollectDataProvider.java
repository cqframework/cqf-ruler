package org.opencds.cqf.ruler.cr.dstu3.provider;

import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.dstu3.Parameters.newPart;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.ListResource.ListEntryComponent;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Reference;
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
	* <a href="http://hl7.org/fhir/DSTU3/clinicalreasoning-module.html">FHIR Clinical Reasoning Module</a>.
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
	@Description(shortDefinition = "$collect-data", value = "Implements the <a href=\"http://hl7.org/fhir/DSTU3/measure-operation-collect-data.html\">$collect-data</a> operation found in the <a href=\"http://hl7.org/fhir/DSTU3/clinicalreasoning-module.html\">FHIR Clinical Reasoning Module</a>.")
	@Operation(name = "$collect-data", idempotent = true, type = Measure.class)
	public Parameters collectData(RequestDetails theRequestDetails, @IdParam IdType theId,
			@OperationParam(name = "periodStart") String periodStart,
			@OperationParam(name = "periodEnd") String periodEnd, @OperationParam(name = "subject") String subject,
			@OperationParam(name = "practitioner") String practitioner,
			@OperationParam(name = "lastReceivedOn") String lastReceivedOn) {

		MeasureReport report = measureEvaluateProvider.evaluateMeasure(theRequestDetails, theId, periodStart, periodEnd,
				"subject", subject, practitioner, lastReceivedOn, null, null);

		// TODO: Data collection doesn't exist?
		report.setType(MeasureReportType.SUMMARY);
		report.setGroup(null);

		Parameters parameters = newParameters(newPart("measureReport", report));

		addEvaluatedResourcesToParameters(report, parameters);

		return parameters;
	}

	private List<Resource> readEvaluatedResources(MeasureReport report) {
		List<Resource> resources = new ArrayList<>();
		if (report.getEvaluatedResources() == null) {
			return resources;
		}

		Reference listReference = report.getEvaluatedResources();

		// Removes the contained "#" prefix

		String listId = listReference.getReference().substring(1);
		Optional<Resource> list = report.getContained().stream().filter(x -> x.getId().equals(listId)).findFirst();

		if(!list.isPresent()) {
			return resources;
		}

		ListResource containedList = (ListResource)list.get();

		for (ListEntryComponent entry : containedList.getEntry()) {
			if (!entry.hasItem()) {
				continue;
			}

			Reference reference = entry.getItem();
			resources.add(this.read(reference.getReferenceElement()));
		}

		return resources;
	}

	private void addEvaluatedResourcesToParameters(MeasureReport report, Parameters parameters) {
		readEvaluatedResources(report)
				.forEach(resource -> parameters.addParameter(newPart("resource", resource)));
	}
}
