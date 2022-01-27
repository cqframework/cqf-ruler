package org.opencds.cqf.ruler.cr.r4.provider;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
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
	public Parameters collectData(RequestDetails theRequestDetails, @IdParam IdType theId, @OperationParam(name = "periodStart") String periodStart,
			  @OperationParam(name = "periodEnd") String periodEnd, @OperationParam(name = "patient") String patientRef,
			  @OperationParam(name = "practitioner") String practitionerRef,
			  @OperationParam(name = "lastReceivedOn") String lastReceivedOn) throws FHIRException {

		 MeasureReport report = measureEvaluateProvider.evaluateMeasure(theRequestDetails, theId, periodStart, periodEnd, null, patientRef, practitionerRef, lastReceivedOn, null);
		 report.setType(MeasureReport.MeasureReportType.DATACOLLECTION);
		 report.setGroup(null);

		 Parameters parameters = new Parameters();
		 parameters.addParameter(new Parameters.ParametersParameterComponent().setName("measureReport").setResource(report));

		 addEvaluatedResourcesToParameters(report, parameters);

		 return parameters;
	}

	private List<IAnyResource> addEvaluatedResources(MeasureReport report){
		 List<IAnyResource> resources = new ArrayList<>();
		 for (Reference evaluatedResource : report.getEvaluatedResource()) {
			  IIdType theEvaluatedId = evaluatedResource.getReferenceElement();
			  String resourceType = theEvaluatedId.getResourceType();
			  if (resourceType != null) {
					IBaseResource resourceBase = getDaoRegistry().getResourceDao(resourceType).read(theEvaluatedId);
					if (resourceBase instanceof Resource) {
						 Resource resource = (Resource) resourceBase;
						 resources.add(resource);
					}
			  }
		 }
		 return resources;
	}

	private void addEvaluatedResourcesToParameters(MeasureReport report, Parameters parameters) {
		 List<IAnyResource> resources;
		 resources = addEvaluatedResources(report);
		 resources.forEach(resource -> {
			  parameters.addParameter(new Parameters.ParametersParameterComponent().setName("resource").setResource((Resource) resource));
			  }
		 );
	}
}
