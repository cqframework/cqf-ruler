package org.opencds.cqf.ruler.cr.r4.provider;

import java.io.IOException;
import java.util.function.Function;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.PlanDefinitionProcessor;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.cr.IRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class PlanDefinitionOperationsProvider implements OperationProvider {

	@Autowired
	IRepositoryFactory myRepositoryFactory;
	@Autowired
	Function<Repository, PlanDefinitionProcessor> myR4PlanDefinitionProcessorFactory;

	private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionOperationsProvider.class);

	@Operation(name = "$apply", idempotent = true, type = PlanDefinition.class)
	public CarePlan applyPlanDefinition(RequestDetails theRequest, @IdParam IdType theId,
			@OperationParam(name = "canonical") String theCanonical,
			@OperationParam(name = "planDefinition") PlanDefinition thePlanDefinition,
			@OperationParam(name = "subject") String theSubject,
			@OperationParam(name = "encounter") String theEncounter,
			@OperationParam(name = "practitioner") String thePractitioner,
			@OperationParam(name = "organization") String theOrganization,
			@OperationParam(name = "userType") String theUserType,
			@OperationParam(name = "userLanguage") String theUserLanguage,
			@OperationParam(name = "userTaskContext") String theUserTaskContext,
			@OperationParam(name = "setting") String theSetting,
			@OperationParam(name = "settingContext") String theSettingContext,
			@OperationParam(name = "parameters") Parameters theParameters,
			@OperationParam(name = "data") Bundle theData,
			@OperationParam(name = "dataEndpoint") Endpoint theDataEndpoint,
			@OperationParam(name = "contentEndpoint") Endpoint theContentEndpoint,
			@OperationParam(name = "terminologyEndpoint") Endpoint theTerminologyEndpoint)
			throws IOException, FHIRException {

		logger.info("Performing $apply operation on PlanDefinition/{}", theId);

		return (CarePlan) this.myR4PlanDefinitionProcessorFactory
				.apply(myRepositoryFactory.create(theRequest))
				.apply(theId,
						new CanonicalType(theCanonical),
						thePlanDefinition,
						theSubject,
						theEncounter,
						thePractitioner,
						theOrganization,
						theUserType,
						theUserLanguage,
						theUserTaskContext,
						theSetting,
						theSettingContext,
						theParameters,
						true,
						theData,
						null,
						theDataEndpoint,
						theContentEndpoint,
						theTerminologyEndpoint);
	}

	@Operation(name = "$r5.apply", idempotent = true, type = PlanDefinition.class)
	public Bundle applyR5PlanDefinition(RequestDetails theRequest, @IdParam IdType theId,
			@OperationParam(name = "canonical") String theCanonical,
			@OperationParam(name = "planDefinition") PlanDefinition thePlanDefinition,
			@OperationParam(name = "subject") String theSubject,
			@OperationParam(name = "encounter") String theEncounter,
			@OperationParam(name = "practitioner") String thePractitioner,
			@OperationParam(name = "organization") String theOrganization,
			@OperationParam(name = "userType") String theUserType,
			@OperationParam(name = "userLanguage") String theUserLanguage,
			@OperationParam(name = "userTaskContext") String theUserTaskContext,
			@OperationParam(name = "setting") String theSetting,
			@OperationParam(name = "settingContext") String theSettingContext,
			@OperationParam(name = "parameters") Parameters theParameters,
			@OperationParam(name = "data") Bundle theData,
			@OperationParam(name = "dataEndpoint") Endpoint theDataEndpoint,
			@OperationParam(name = "contentEndpoint") Endpoint theContentEndpoint,
			@OperationParam(name = "terminologyEndpoint") Endpoint theTerminologyEndpoint)
			throws IOException, FHIRException {

		logger.info("Performing $r5.apply operation on PlanDefinition/{}", theId);

		return (Bundle) this.myR4PlanDefinitionProcessorFactory
				.apply(myRepositoryFactory.create(theRequest))
				.applyR5(theId,
						new CanonicalType(theCanonical),
						thePlanDefinition,
						theSubject,
						theEncounter,
						thePractitioner,
						theOrganization,
						theUserType,
						theUserLanguage,
						theUserTaskContext,
						theSetting,
						theSettingContext,
						theParameters,
						true,
						theData,
						null,
						theDataEndpoint,
						theContentEndpoint,
						theTerminologyEndpoint);
	}
}
