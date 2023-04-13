package org.opencds.cqf.ruler.cr.r4.provider;

import java.util.function.Function;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.ruler.behavior.ResourceCreator;
import org.opencds.cqf.ruler.cr.IRepositoryFactory;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

/**
 * Created by Bryn on 1/16/2017.
 */
public class ActivityDefinitionOperationsProvider extends DaoRegistryOperationProvider implements ResourceCreator {

	@Autowired
	IRepositoryFactory myRepositoryFactory;
	@Autowired
	Function<Repository, ActivityDefinitionProcessor> myR4ActivityDefinitionProcessorFactory;

	@Operation(name = "$apply", idempotent = true, type = ActivityDefinition.class)
	public Resource apply(RequestDetails theRequest, @IdParam IdType theId,
			@OperationParam(name = "patient") String patientId,
			@OperationParam(name = "encounter") String encounterId,
			@OperationParam(name = "practitioner") String practitionerId,
			@OperationParam(name = "organization") String organizationId,
			@OperationParam(name = "userType") String userType,
			@OperationParam(name = "userLanguage") String userLanguage,
			@OperationParam(name = "userTaskContext") String userTaskContext,
			@OperationParam(name = "setting") String setting,
			@OperationParam(name = "settingContext") String settingContext)
			throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		return (Resource) myR4ActivityDefinitionProcessorFactory.apply(myRepositoryFactory.create(theRequest))
				.apply(theId, null, null, patientId, encounterId, practitionerId, organizationId, userType,
						userLanguage, userTaskContext, setting, settingContext, null, null, null, null);
	}
}
