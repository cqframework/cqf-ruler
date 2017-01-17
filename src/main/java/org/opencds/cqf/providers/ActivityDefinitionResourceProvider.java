package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.cql.data.fhir.JpaFhirDataProvider;

import java.util.Collection;

/**
 * Created by Bryn on 1/16/2017.
 */
public class ActivityDefinitionResourceProvider extends JpaResourceProviderDstu3<ActivityDefinition> {

    private JpaFhirDataProvider provider;
    private CqlExecutionProvider executionProvider;

    public ActivityDefinitionResourceProvider(Collection<IResourceProvider> providers) {
        this.provider = new JpaFhirDataProvider(providers);
        this.executionProvider = new CqlExecutionProvider(providers);
    }

    @Operation(name = "$apply", idempotent = true)
    public Resource apply(@IdParam IdType theId, @RequiredParam(name="patient") String patientId,
                          @OptionalParam(name="encounter") String encounterId,
                          @OptionalParam(name="practitioner") String practitionerId,
                          @OptionalParam(name="organization") String organizationId,
                          @OptionalParam(name="userType") String userType,
                          @OptionalParam(name="userLanguage") String userLanguage,
                          @OptionalParam(name="userTaskContext") String userTaskContext,
                          @OptionalParam(name="setting") String setting,
                          @OptionalParam(name="settingContext") String settingContext)
            throws InternalErrorException, FHIRException {
        ActivityDefinition activityDefinition = this.getDao().read(theId);
        Resource result = null;
        switch (activityDefinition.getCategory()) {
            case COMMUNICATION:
                result = new CommunicationRequest();
                break;
            case DEVICE:
                result = new DeviceUseRequest();
                break;
            case DIAGNOSTIC:
                result = new DiagnosticRequest();
                break;
            case DIET:
                result = new NutritionRequest();
                break;
            case DRUG:
                result = new MedicationRequest();
                break;
            case ENCOUNTER:
                result = new Appointment();
                break;
            case IMMUNIZATION:
                result = new ImmunizationRecommendation();
                break;
            case OBSERVATION:
                result = new Observation();
                break;
            case PROCEDURE:
                result = new ProcedureRequest();
                break;
            case REFERRAL:
                result = new ReferralRequest();
                break;
            case SUPPLY:
                result = new SupplyRequest();
                break;
            case VISION:
                result = new VisionPrescription();
                break;
            case OTHER:
            default:
                throw new RuntimeException(String.format("Unknown or unimplemented activity definition category %s.", activityDefinition.getCategory()));
        }

        // TODO: Apply metadata, code, timing, location, participants, product, quantity, dosageInstruction, bodySite, etc....
        // TODO: Apply expression extensions on any element?

        for (ActivityDefinition.ActivityDefinitionDynamicValueComponent dynamicValue : activityDefinition.getDynamicValue()) {
            if (dynamicValue.getExpression() != null) {
                // TODO: Passing the activityDefinition as context here because that's what will have the libraries, but perhaps the "context" here should be the result resource?
                Object value = executionProvider.evaluateInContext(activityDefinition, dynamicValue.getExpression());
                this.provider.setValue(result, dynamicValue.getPath(), value);
            }
        }

        return result;
    }
}
