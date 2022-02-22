package org.opencds.cqf.ruler.behavior.r4;

import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.behavior.IdCreator;
import org.opencds.cqf.ruler.utility.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MeasureReportUser extends DaoRegistryUser, IdCreator {
	static final Logger ourLog = LoggerFactory.getLogger(ParameterUser.class);

	public static final String MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM = "http://terminology.hl7.org/CodeSystem/measure-improvement-notation";
	public static final String MEASUREREPORT_MEASURE_POPULATION_SYSTEM = "http://terminology.hl7.org/CodeSystem/measure-population";

	public default Map<String, Resource> getEvaluatedResources(org.hl7.fhir.r4.model.MeasureReport report) {
		Map<String, Resource> resources = new HashMap<>();
		getEvaluatedResources(report, resources);

		return resources;
	}

	public default MeasureReportUser getEvaluatedResources(org.hl7.fhir.r4.model.MeasureReport report,
			Map<String, Resource> resources) {
		report.getEvaluatedResource().forEach(evaluatedResource -> {
			IIdType resourceId = evaluatedResource.getReferenceElement();
			if (resourceId.getResourceType() == null || resources.containsKey(Ids.simple(resourceId))) {
				return;
			}
			IBaseResource resourceBase = read(resourceId);
			if (resourceBase instanceof Resource) {
				Resource resource = (Resource) resourceBase;
				resources.put(Ids.simple(resourceId), resource);
			}
		});

		return this;
	}
}
