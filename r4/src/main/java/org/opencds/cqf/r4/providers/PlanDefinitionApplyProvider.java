package org.opencds.cqf.r4.providers;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.r4.processors.PlanDefinitionApplyProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;

import ca.uhn.fhir.jpa.dao.DaoRegistry;
public class PlanDefinitionApplyProvider {
    
    private PlanDefinitionApplyProcessor planDefinitionApplyProcessor;

    public static final Logger logger = LoggerFactory.getLogger(PlanDefinitionApplyProvider.class);

    public PlanDefinitionApplyProvider(FhirContext fhirContext, ActivityDefinitionApplyProvider activitydefinitionApplyProvider, DaoRegistry registry,
    CqlExecutionProvider executionProvider) {
        planDefinitionApplyProcessor = new PlanDefinitionApplyProcessor(fhirContext, activitydefinitionApplyProvider, registry, executionProvider);
    }

    @Operation(name = "$apply", idempotent = true, type = PlanDefinition.class)
    public CarePlan applyPlanDefinition(
            @IdParam IdType theId,
            @RequiredParam(name="patient") String patientId,
            @OptionalParam(name="encounter") String encounterId,
            @OptionalParam(name="practitioner") String practitionerId,
            @OptionalParam(name="organization") String organizationId,
            @OptionalParam(name="userType") String userType,
            @OptionalParam(name="userLanguage") String userLanguage,
            @OptionalParam(name="userTaskContext") String userTaskContext,
            @OptionalParam(name="setting") String setting,
            @OptionalParam(name="settingContext") String settingContext,
            @OperationParam(name="artifactEndpoint") Endpoint artifactEndpoint)
        throws IOException, JAXBException, FHIRException {
        
        //for now defaulting to localhost, but I believe this should be set to the server address either using the config or endpoint initialized after running server.
        if(artifactEndpoint == null) {
            artifactEndpoint = new Endpoint();
            artifactEndpoint.setAddress("http://localhost:8080/cqf-ruler-r4/fhir/");
        }
        return planDefinitionApplyProcessor.applyPlanDefinition(theId, patientId, encounterId, practitionerId, organizationId, userType, userLanguage, userTaskContext, setting, settingContext, artifactEndpoint);
    }

	public PlanDefinitionApplyProcessor getProcessor() {
		return planDefinitionApplyProcessor;
	}    
}