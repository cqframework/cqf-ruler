package org.opencds.cqf.r4.providers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

// import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
// import ca.uhn.fhir.validation.FhirValidator;
// import ca.uhn.fhir.validation.IValidatorModule;
// import ca.uhn.fhir.validation.ValidationResult;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
// import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
// import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;

@Component
public class ProcessMessageProvider {
    private static final Logger logger = LoggerFactory.getLogger(ProcessMessageProvider.class);
    private final FhirContext fhirContext;
    private final DaoRegistry registry;

    @Inject
    public ProcessMessageProvider(FhirContext fhirContext, DaoRegistry registry) {
        this.fhirContext = fhirContext;
        this.registry = registry;
    }

	@SuppressWarnings("unchecked") 
    @Operation(name = "$process-message-bundle", idempotent = false)
	public Bundle processMessageBundle(HttpServletRequest theServletRequest, RequestDetails theRequestDetails,
			@OperationParam(name = "content", min = 1, max = 1) @Description(formalDefinition = "The message to process (or, if using asynchronous messaging, it may be a response message to accept)") Bundle theMessageToProcess) {
		logger.info("Validating the Bundle");
		Bundle bundle = theMessageToProcess;
		boolean errorExists = false;
		try {
			OperationOutcome outcome = validateBundle(errorExists, bundle);
			if (!errorExists) {
                IVersionSpecificBundleFactory bundleFactory = fhirContext.newBundleFactory();
				bundle.setId(getUUID());
                bundleFactory.initializeWithBundleResource(bundle);
                Bundle dafBundle = (Bundle) bundleFactory.getResourceBundle();
				dafBundle.setTimestamp(new Date());
                registry.getResourceDao(Bundle.class).create(dafBundle);

				MessageHeader messageHeader = null;
				String patientId = null;
				String commId =null;
				for(BundleEntryComponent entryComp: bundle.getEntry()) {
					registry.getResourceDao(entryComp.getResource().fhirType()).create(entryComp.getResource());
					if(entryComp.getResource().getResourceType().name().equals("MessageHeader")) {
						messageHeader = (MessageHeader) entryComp.getResource();
						messageHeader.setId(getUUID());
						Meta meta = messageHeader.getMeta();
						meta.setLastUpdated(new Date());
						messageHeader.setMeta(meta);
					} else if(entryComp.getResource().getResourceType().name().equals("Bundle")) {
						Bundle innerBundle = (Bundle) entryComp.getResource();
						for(BundleEntryComponent bundleEntryComponent:innerBundle.getEntry()) {
							registry.getResourceDao(bundleEntryComponent.getResource().fhirType()).create(bundleEntryComponent.getResource());
							if(bundleEntryComponent.getResource().getResourceType().name().equals("Patient")) {
								patientId = bundleEntryComponent.getResource().getIdElement().getIdPart().toString();
							}
						}
					}
				}
				if(patientId!= null) {
					commId = constructAndSaveCommunication(patientId);	
				}
				if(messageHeader == null) {
					messageHeader = constructMessageHeaderResource();	
					BundleEntryComponent entryComp = new BundleEntryComponent();
					entryComp.setResource(messageHeader);
					dafBundle.addEntry(entryComp);
				}
				if(commId != null) {
					List<Reference> referenceList = new ArrayList<Reference>();
					Reference commRef = new Reference();
					commRef.setReference("Communication/"+commId);
					referenceList.add(commRef);
					messageHeader.setFocus(referenceList);	
				}
				return dafBundle;
			} else {
				BundleEntryComponent entryComp = new BundleEntryComponent();
				entryComp.setResource(outcome);
				bundle.addEntry(entryComp);
				return bundle;
			}
		} catch (Exception e) {
			throw new UnprocessableEntityException("Error in Processing the Bundle");
		}
	}

	private OperationOutcome validateBundle(boolean errorExists, Bundle bundle) {
		// FhirValidator validator = fhirContext.newValidator();
		OperationOutcome outcome = new OperationOutcome();
		// // Create a validation module and register it
		// IValidatorModule module = new FhirInstanceValidator(fhirContext);
		// validator.registerValidatorModule(module);
		// ValidationResult result = validator.validateWithResult(bundle);
		// outcome = (OperationOutcome) result.toOperationOutcome();
		// if (outcome.hasIssue()) {
		// 	List<OperationOutcomeIssueComponent> issueCompList = outcome.getIssue();
		// 	for (OperationOutcomeIssueComponent issueComp : issueCompList) {
		// 		if (issueComp.getSeverity().equals(IssueSeverity.ERROR)) {
		// 			errorExists = true;
		// 		}
		// 	}
		// }
		return outcome;
	}

	private MessageHeader constructMessageHeaderResource() {
		String message = "{\"resourceType\": \"MessageHeader\",\"id\": \"messageheader-example-reportheader\",\"meta\": {\"versionId\": \"1\",\"lastUpdated\": \"2020-11-29T02:03:28.045+00:00\",\"profile\": [\"http://hl7.org/fhir/us/medmorph/StructureDefinition/us-ph-messageheader\"]},\"extension\": [{\"url\": \"http://hl7.org/fhir/us/medmorph/StructureDefinition/ext-dataEncrypted\",\"valueBoolean\": false},{\"url\":\"http://hl7.org/fhir/us/medmorph/StructureDefinition/ext-messageProcessingCategory\",\"valueCode\": \"consequence\"}],\"eventCoding\": {\"system\": \"http://hl7.org/fhir/us/medmorph/CodeSystem/us-ph-messageheader-message-types\",\"code\": \"cancer-report-message\"},\"destination\": [{\"name\": \"PHA endpoint\",\"endpoint\": \"http://example.pha.org/fhir\"}],\"source\": {\"name\": \"Healthcare Organization\",\"software\": \"Backend Service App\",\"version\": \"3.1.45.AABB\",\"contact\": {\"system\": \"phone\",\"value\": \"+1 (917) 123 4567\"},\"endpoint\": \"http://example.healthcare.org/fhir\"},\"reason\": {\"coding\": [{\"system\": \"http://hl7.org/fhir/us/medmorph/CodeSystem/us-ph-triggerdefinition-namedevents\",\"code\": \"encounter-close\"}]}}";
		MessageHeader messageHeader = (MessageHeader) fhirContext.newJsonParser().parseResource(message);
		messageHeader.setId(getUUID());
		return messageHeader;
	}
	
	private String constructAndSaveCommunication(String patientId) {
		String communication ="{\"resourceType\" : \"Communication\",\"meta\" : {\"versionId\" : \"1\",\"profile\" : [\"http://hl7.org/fhir/us/medmorph/StructureDefinition/us-ph-communication\"]},\"extension\" : [{\"url\" : \"http://hl7.org/fhir/us/medmorph/StructureDefinition/ext-responseMessageStatus\",\"valueCodeableConcept\" : {\"coding\" : [{\"system\" :\"http://hl7.org/fhir/us/medmorph/CodeSystem/us-ph-response-message-processing-status\",\"code\" : \"RRVS1\"}]}}],\"identifier\" : [{\"system\" : \"http://example.pha.org/\",\"value\" : \"12345\"}],\"status\" : \"completed\",\"category\" : [{\"coding\" : [{\"system\" : \"http://hl7.org/fhir/us/medmorph/CodeSystem/us-ph-messageheader-message-types\",\"code\" : \"cancer-response-message\"}]}],\"reasonCode\" : [{\"coding\" : [{\"system\" : \"http://hl7.org/fhir/us/medmorph/CodeSystem/us-ph-messageheader-message-types\",\"code\" : \"cancer-report-message\"}]}]}";
		Communication comm = (Communication) fhirContext.newJsonParser().parseResource(communication);
		String commId = getUUID();
		comm.setId(commId);
		Meta meta = comm.getMeta();
		meta.setLastUpdated(new Date());
		comm.setMeta(meta);
		comm.setSubject(new Reference("Patient/"+patientId));
		comm.setReceived(new Date());
        registry.getResourceDao(Communication.class).create(comm);
		return commId;
	}

	public String getUUID() {
	    UUID uuid = UUID.randomUUID();
	    String randomUUID = uuid.toString();
	    return randomUUID;
	}
}
