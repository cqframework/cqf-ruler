package org.opencds.cqf.ruler.cr.dstu3.provider;

import java.io.IOException;
import java.util.function.Function;

import org.hl7.fhir.dstu3.model.CarePlan;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.cr.dstu3.service.PlanDefinitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class PlanDefinitionApplyProvider implements OperationProvider {

	@Autowired
	Function<RequestDetails, PlanDefinitionService> dstu3PlanDefinitionServiceFactory;

	private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionApplyProvider.class);

	@Operation(name = "$apply", idempotent = true, type = PlanDefinition.class)
	public CarePlan applyPlanDefinition(RequestDetails theRequest,
			@IdParam IdType theId, @OperationParam(name = "patient") String patientId,
			@OperationParam(name = "encounter") String encounterId,
			@OperationParam(name = "practitioner") String practitionerId,
			@OperationParam(name = "organization") String organizationId,
			@OperationParam(name = "userType") String userType,
			@OperationParam(name = "userLanguage") String userLanguage,
			@OperationParam(name = "userTaskContext") String userTaskContext,
			@OperationParam(name = "setting") String setting,
			@OperationParam(name = "settingContext") String settingContext)
			throws IOException, FHIRException {

		logger.info("Performing $apply operation on PlanDefinition/{}", theId);

		return this.dstu3PlanDefinitionServiceFactory
				.apply(theRequest)
				.applyPlanDefinition(
						theId,
						patientId,
						encounterId,
						practitionerId,
						organizationId,
						userType,
						userLanguage,
						userTaskContext,
						setting,
						settingContext);
	}
}
