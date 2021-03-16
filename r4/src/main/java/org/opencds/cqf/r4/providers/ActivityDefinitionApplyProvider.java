package org.opencds.cqf.r4.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.CommunicationRequest;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.SupplyRequest;
import org.hl7.fhir.r4.model.Task;
import org.opencds.cqf.common.exceptions.ActivityDefinitionApplyException;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.r4.builders.JavaDateBuilder;
import org.opencds.cqf.r4.helpers.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

/**
 * Created by Bryn on 1/16/2017.
 */
@Component
public class ActivityDefinitionApplyProvider {

    private CqlExecutionProvider executionProvider;
    private ModelResolver modelResolver;
    private IFhirResourceDao<ActivityDefinition> activityDefinitionDao;

    private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionApplyProvider.class);

    @Inject
    public ActivityDefinitionApplyProvider(FhirContext fhirContext, CqlExecutionProvider executionProvider,
            IFhirResourceDao<ActivityDefinition> activityDefinitionDao) {
        this.modelResolver = new R4FhirModelResolver();
        this.executionProvider = executionProvider;
        this.activityDefinitionDao = activityDefinitionDao;
    }

    @Operation(name = "$apply", idempotent = true, type = ActivityDefinition.class)
    public Resource apply(@IdParam IdType theId, @OperationParam(name = "patient") String patientId,
            @OperationParam(name = "encounter") String encounterId,
            @OperationParam(name = "practitioner") String practitionerId,
            @OperationParam(name = "organization") String organizationId,
            @OperationParam(name = "userType") String userType,
            @OperationParam(name = "userLanguage") String userLanguage,
            @OperationParam(name = "userTaskContext") String userTaskContext,
            @OperationParam(name = "setting") String setting,
            @OperationParam(name = "settingContext") String settingContext)
            throws InternalErrorException, FHIRException, ClassNotFoundException, IllegalAccessException,
            InstantiationException, ActivityDefinitionApplyException {
        ActivityDefinition activityDefinition;

        try {
            activityDefinition = this.activityDefinitionDao.read(theId);
        } catch (Exception e) {
            return Helper.createErrorOutcome("Unable to resolve ActivityDefinition/" + theId.getValueAsString());
        }

        return resolveActivityDefinition(activityDefinition, patientId, practitionerId, organizationId);
    }

    // For library use
    public Resource resolveActivityDefinition(ActivityDefinition activityDefinition, String patientId,
            String practitionerId, String organizationId) throws FHIRException {
        Resource result = null;
        try {
            result = (Resource) Class.forName("org.hl7.fhir.r4.model." + activityDefinition.getKind().toCode()).getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            throw new FHIRException("Could not find org.hl7.fhir.r4.model." + activityDefinition.getKind().toCode());
        }

        switch (result.fhirType()) {
            case "ServiceRequest":
                result = resolveServiceRequest(activityDefinition, patientId, practitionerId, organizationId);
                break;

            case "MedicationRequest":
                result = resolveMedicationRequest(activityDefinition, patientId);
                break;

            case "SupplyRequest":
                result = resolveSupplyRequest(activityDefinition, practitionerId, organizationId);
                break;

            case "Procedure":
                result = resolveProcedure(activityDefinition, patientId);
                break;

            case "DiagnosticReport":
                result = resolveDiagnosticReport(activityDefinition, patientId);
                break;

            case "Communication":
                result = resolveCommunication(activityDefinition, patientId);
                break;

            case "CommunicationRequest":
                result = resolveCommunicationRequest(activityDefinition, patientId);
                break;

            case "Task":
                result = resolveTask(activityDefinition, patientId);
                break;
        }

        // TODO: Apply expression extensions on any element?

        for (ActivityDefinition.ActivityDefinitionDynamicValueComponent dynamicValue : activityDefinition
                .getDynamicValue()) {

            // Catch errors while setting each dynamicValue so that entire ActivityDefinition apply operation does not fail.
            try {
	        	if (dynamicValue.getExpression() != null) {
	            	// Special case for setting a Patient reference
	            	if ("Patient".equals(dynamicValue.getExpression().getExpression())) {
                    	this.modelResolver.setValue(result, dynamicValue.getPath(), new Reference(patientId));
	                    	
	            	}
	            	else {
		                /*
		                 * TODO: Passing the activityDefinition as context here because that's what will
		                 * have the libraries, but perhaps the "context" here should be the result
		                 * resource?
		                 */
		                Object value = executionProvider.evaluateInContext(activityDefinition,
		                        dynamicValue.getExpression().getExpression(), patientId);
		
		                if (value != null) {
		                	logger.debug("dynamicValue value: " + value.toString());
			                if (value instanceof Boolean) {
			                    value = new BooleanType((Boolean) value);
			                }
			                else if (value instanceof DateTime) {
			                	value = new JavaDateBuilder().buildFromDateTime((DateTime) value).build();
			                }
			                else if (value instanceof String) {
			                	value = new StringType((String) value);
			                }
			                
			                this.modelResolver.setValue(result, dynamicValue.getPath(), value);
		
		                }
		                else {
		                	logger.warn("WARNING: ActivityDefinition has null value for path: " + dynamicValue.getPath());
		                }
	            	}
	            }
	        	
            } catch (Exception e) {
                logger.error("ERROR: ActivityDefinition dynamicValue %s could not be applied and threw exception %s",
                		dynamicValue.getPath(), e.toString());
                logger.error(e.toString());
            }
        }

        return result;
    }

    private ServiceRequest resolveServiceRequest(ActivityDefinition activityDefinition, String patientId,
            String practitionerId, String organizationId) throws ActivityDefinitionApplyException {
        // status, intent, code, and subject are required
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
        serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        serviceRequest.setSubject(new Reference(patientId));

        if (practitionerId != null) {
            serviceRequest.setRequester(new Reference(practitionerId));
        }

        else if (organizationId != null) {
            serviceRequest.setRequester(new Reference(organizationId));
        }

        if (activityDefinition.hasExtension()) {
            serviceRequest.setExtension(activityDefinition.getExtension());
        }

        if (activityDefinition.hasCode()) {
            serviceRequest.setCode(activityDefinition.getCode());
        }

        // code can be set as a dynamicValue
        else if (!activityDefinition.hasCode() && !activityDefinition.hasDynamicValue()) {
            throw new ActivityDefinitionApplyException("Missing required code property");
        }

        if (activityDefinition.hasBodySite()) {
            serviceRequest.setBodySite(activityDefinition.getBodySite());
        }

        if (activityDefinition.hasProduct()) {
            throw new ActivityDefinitionApplyException("Product does not map to " + activityDefinition.getKind());
        }

        if (activityDefinition.hasDosage()) {
            throw new ActivityDefinitionApplyException("Dosage does not map to " + activityDefinition.getKind());
        }

        return serviceRequest;
    }

    private MedicationRequest resolveMedicationRequest(ActivityDefinition activityDefinition, String patientId)
            throws ActivityDefinitionApplyException {
        // intent, medication, and subject are required
        MedicationRequest medicationRequest = new MedicationRequest();
        medicationRequest.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
        medicationRequest.setSubject(new Reference(patientId));

        if (activityDefinition.hasProduct()) {
            medicationRequest.setMedication(activityDefinition.getProduct());
        }

        else {
            throw new ActivityDefinitionApplyException("Missing required product property");
        }

        if (activityDefinition.hasDosage()) {
            medicationRequest.setDosageInstruction(activityDefinition.getDosage());
        }

        if (activityDefinition.hasBodySite()) {
            throw new ActivityDefinitionApplyException("BodySite does not map to " + activityDefinition.getKind());
        }

        if (activityDefinition.hasCode()) {
            throw new ActivityDefinitionApplyException("Code does not map to " + activityDefinition.getKind());
        }

        if (activityDefinition.hasQuantity()) {
            throw new ActivityDefinitionApplyException("Quantity does not map to " + activityDefinition.getKind());
        }

        return medicationRequest;
    }

    private SupplyRequest resolveSupplyRequest(ActivityDefinition activityDefinition, String practitionerId,
            String organizationId) throws ActivityDefinitionApplyException {
        SupplyRequest supplyRequest = new SupplyRequest();

        if (practitionerId != null) {
            supplyRequest.setRequester(new Reference(practitionerId));
        }

        if (organizationId != null) {
            supplyRequest.setRequester(new Reference(organizationId));
        }

        if (activityDefinition.hasQuantity()) {
            supplyRequest.setQuantity(activityDefinition.getQuantity());
        }

        else {
            throw new ActivityDefinitionApplyException("Missing required orderedItem.quantity property");
        }

        if (activityDefinition.hasCode()) {
            supplyRequest.setItem(activityDefinition.getCode());
        }

        if (activityDefinition.hasProduct()) {
            throw new ActivityDefinitionApplyException("Product does not map to " + activityDefinition.getKind());
        }

        if (activityDefinition.hasDosage()) {
            throw new ActivityDefinitionApplyException("Dosage does not map to " + activityDefinition.getKind());
        }

        if (activityDefinition.hasBodySite()) {
            throw new ActivityDefinitionApplyException("BodySite does not map to " + activityDefinition.getKind());
        }

        return supplyRequest;
    }

    private Procedure resolveProcedure(ActivityDefinition activityDefinition, String patientId) {
        Procedure procedure = new Procedure();

        // TODO - set the appropriate status
        procedure.setStatus(Procedure.ProcedureStatus.UNKNOWN);
        procedure.setSubject(new Reference(patientId));

        if (activityDefinition.hasCode()) {
            procedure.setCode(activityDefinition.getCode());
        }

        if (activityDefinition.hasBodySite()) {
            procedure.setBodySite(activityDefinition.getBodySite());
        }

        return procedure;
    }

    private DiagnosticReport resolveDiagnosticReport(ActivityDefinition activityDefinition, String patientId) {
        DiagnosticReport diagnosticReport = new DiagnosticReport();

        diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.UNKNOWN);
        diagnosticReport.setSubject(new Reference(patientId));

        if (activityDefinition.hasCode()) {
            diagnosticReport.setCode(activityDefinition.getCode());
        }

        else {
            throw new ActivityDefinitionApplyException(
                    "Missing required ActivityDefinition.code property for DiagnosticReport");
        }

        if (activityDefinition.hasRelatedArtifact()) {
            List<Attachment> presentedFormAttachments = new ArrayList<>();
            for (RelatedArtifact artifact : activityDefinition.getRelatedArtifact()) {
                Attachment attachment = new Attachment();

                if (artifact.hasUrl()) {
                    attachment.setUrl(artifact.getUrl());
                }

                if (artifact.hasDisplay()) {
                    attachment.setTitle(artifact.getDisplay());
                }
                presentedFormAttachments.add(attachment);
            }
            diagnosticReport.setPresentedForm(presentedFormAttachments);
        }

        return diagnosticReport;
    }

    private Communication resolveCommunication(ActivityDefinition activityDefinition, String patientId) {
        Communication communication = new Communication();

        communication.setStatus(Communication.CommunicationStatus.UNKNOWN);
        communication.setSubject(new Reference(patientId));

        if (activityDefinition.hasCode()) {
            communication.setReasonCode(Collections.singletonList(activityDefinition.getCode()));
        }

        if (activityDefinition.hasRelatedArtifact()) {
            for (RelatedArtifact artifact : activityDefinition.getRelatedArtifact()) {
                if (artifact.hasUrl()) {
                    Attachment attachment = new Attachment().setUrl(artifact.getUrl());
                    if (artifact.hasDisplay()) {
                        attachment.setTitle(artifact.getDisplay());
                    }

                    Communication.CommunicationPayloadComponent payload = new Communication.CommunicationPayloadComponent();
                    payload.setContent(artifact.hasDisplay() ? attachment.setTitle(artifact.getDisplay()) : attachment);
                    communication.setPayload(Collections.singletonList(payload));
                }

                // TODO - other relatedArtifact types
            }
        }

        return communication;
    }

    private CommunicationRequest resolveCommunicationRequest(ActivityDefinition activityDefinition, String patientId) {
        CommunicationRequest communicationRequest = new CommunicationRequest();

        communicationRequest.setStatus(CommunicationRequest.CommunicationRequestStatus.UNKNOWN);
        communicationRequest.setSubject(new Reference(patientId));

        // Unsure if this is correct - this is the way Motive is doing it...
        if (activityDefinition.hasCode()) {
            if (activityDefinition.getCode().hasText()) {
                communicationRequest.addPayload().setContent(new StringType(activityDefinition.getCode().getText()));
            }
        }

        if (activityDefinition.hasRelatedArtifact()) {
            for (RelatedArtifact artifact : activityDefinition.getRelatedArtifact()) {
                if (artifact.hasUrl()) {
                    Attachment attachment = new Attachment().setUrl(artifact.getUrl());
                    if (artifact.hasDisplay()) {
                        attachment.setTitle(artifact.getDisplay());
                    }

                    CommunicationRequest.CommunicationRequestPayloadComponent payload = new CommunicationRequest.CommunicationRequestPayloadComponent();
                    payload.setContent(artifact.hasDisplay() ? attachment.setTitle(artifact.getDisplay()) : attachment);
                    communicationRequest.setPayload(Collections.singletonList(payload));
                }

                // TODO - other relatedArtifact types
            }
        }

        return communicationRequest;
    }

    private Task resolveTask(ActivityDefinition activityDefinition, String patientId) {
    	Task task = new Task();

    	task.setStatus(Task.TaskStatus.DRAFT);
    	task.setIntent(Task.TaskIntent.PROPOSAL);
    	task.setFor(new Reference(patientId));

        Task.ParameterComponent input = new Task.ParameterComponent();
        
        if (activityDefinition.hasCode()) {
            task.setCode(activityDefinition.getCode());
            input.setType(activityDefinition.getCode());
        }
        
        // Extension defined by CPG-on-FHIR for Questionnaire canonical URI
        Extension collectsWith = activityDefinition.getExtensionByUrl("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-collectWith");
        if (collectsWith != null && collectsWith.getValueAsPrimitive().toString() != null) {
        	CanonicalType uri = new CanonicalType(collectsWith.getValueAsPrimitive().toString());
        	input.setValue(uri);
        }

        if (activityDefinition.hasRelatedArtifact()) {
            for (RelatedArtifact artifact : activityDefinition.getRelatedArtifact()) {
                if (artifact.hasUrl()) {
                    Attachment attachment = new Attachment().setUrl(artifact.getUrl());
                    if (artifact.hasDisplay()) {
                        attachment.setTitle(artifact.getDisplay());
                    }

                    input.setValue(artifact.hasDisplay() ? attachment.setTitle(artifact.getDisplay()) : attachment);
                }

                // TODO - other relatedArtifact types
            }
        }
        
        // If input has been populated, then add it to the Task.
        if (input.getType() != null || input.getValue() != null) {
        	task.addInput(input);
        }

        return task;
    }
}
