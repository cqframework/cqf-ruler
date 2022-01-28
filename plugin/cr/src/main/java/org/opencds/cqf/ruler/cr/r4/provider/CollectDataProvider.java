package org.opencds.cqf.ruler.cr.r4.provider;

import static org.opencds.cqf.ruler.utility.r4.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newPart;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class CollectDataProvider extends DaoRegistryOperationProvider {

	@Autowired
	private MeasureEvaluateProvider measureEvaluateProvider;

	@Operation(name = "$collect-data", idempotent = true, type = Measure.class)
	public Parameters collectData(RequestDetails theRequestDetails, @IdParam IdType theId,
			@OperationParam(name = "periodStart") String periodStart,
			@OperationParam(name = "periodEnd") String periodEnd, @OperationParam(name = "patient") String patientRef,
			@OperationParam(name = "practitioner") String practitionerRef,
			@OperationParam(name = "lastReceivedOn") String lastReceivedOn) {

		MeasureReport report = measureEvaluateProvider.evaluateMeasure(theRequestDetails, theId, periodStart, periodEnd,
				null, patientRef, practitionerRef, lastReceivedOn, null);
		report.setType(MeasureReport.MeasureReportType.DATACOLLECTION);
		report.setGroup(null);

		Parameters parameters = newParameters(newPart("measureReport", report));

		addEvaluatedResourcesToParameters(report, parameters);

		return parameters;
	}

	private List<Resource> readEvaluatedResources(MeasureReport report) {
		List<Resource> resources = new ArrayList<>();
		for (Reference reference : report.getEvaluatedResource()) {
			Resource resource = read(reference.getReferenceElement());
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
