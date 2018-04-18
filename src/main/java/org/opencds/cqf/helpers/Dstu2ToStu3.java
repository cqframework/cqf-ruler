package org.opencds.cqf.helpers;

import org.hl7.fhir.convertors.NullVersionConverterAdvisor30;
import org.hl7.fhir.convertors.VersionConvertor_10_30;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.MedicationOrder;

public class Dstu2ToStu3 {

    public static Resource convertResource(org.hl7.fhir.instance.model.Resource resource) {
        NullVersionConverterAdvisor30 advisor = new NullVersionConverterAdvisor30();
        VersionConvertor_10_30 converter = new VersionConvertor_10_30(advisor);

        // TODO - need to convert DiagnosticOrder to ProcedureRequest, and DeviceUseRequest to DeviceRequest
        try {
            switch (resource.getResourceType().name()) {
                case "MedicationOrder":
                    return resolveMedicationRequest((MedicationOrder) resource, converter);
                case "Bundle":
                    return resolveBundle((org.hl7.fhir.instance.model.Bundle) resource);
                default:
                    return converter.convertResource(resource);
            }
        } catch (FHIRException fe) {
            fe.printStackTrace();
            throw new RuntimeException("Error converting type: " + resource.getResourceType().name() + "\nMessage: " + fe.getMessage());
        }
    }

    // TODO - complete conversion - only interested in the type and entry resources currently
    public static Bundle resolveBundle(org.hl7.fhir.instance.model.Bundle bundle) {
        Bundle returnBundle = new Bundle();
        try {
            returnBundle.setType(Bundle.BundleType.fromCode(bundle.getType().toCode()));
        } catch (FHIRException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        Bundle.BundleEntryComponent returnEntry = new Bundle.BundleEntryComponent();
        for (org.hl7.fhir.instance.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource()) {
                returnEntry.setResource(convertResource(entry.getResource()));
                returnBundle.addEntry(returnEntry);
            }
        }
        return returnBundle;
    }

    // MedicationRequest
    public static MedicationRequest resolveMedicationRequest(MedicationOrder order, VersionConvertor_10_30 converter) throws FHIRException {
        /*
        *   Required fields:
        *   MedicationOrder              -> MedicationRequest
        *   status                       -> status
        *   null                         -> intent (default to order)
        *   medication                   -> medication (assuming CodeableConcept)
        *   dateWritten                  -> authoredOn
        *   encounter                    -> context
        *   dosageInstruction (Backbone) -> Dosage (Element)
        *   dispenseRequest              -> dispenseRequest
        */
        MedicationRequest request =
                new MedicationRequest()
                        .setStatus(order.hasStatus() ? MedicationRequest.MedicationRequestStatus.fromCode(order.getStatus().toCode()) : MedicationRequest.MedicationRequestStatus.UNKNOWN)
                        .setIntent(MedicationRequest.MedicationRequestIntent.ORDER)
                        .setMedication(converter.convertCodeableConcept(order.getMedicationCodeableConcept()))
                        .setDispenseRequest(convertDispenseRequest(order.getDispenseRequest()));

        for (MedicationOrder.MedicationOrderDosageInstructionComponent dosageInstruction : order.getDosageInstruction()) {
            request.addDosageInstruction(converter.convertMedicationOrderDosageInstructionComponent(dosageInstruction));
        }

        if (order.hasDateWritten()) {
            request.setAuthoredOn(order.getDateWritten());
        }

        if (order.hasEncounter()) {
            request.setContext(new Reference().setReference(order.getEncounter().getReference()));
        }

        return request;
    }

    private static MedicationRequest.MedicationRequestDispenseRequestComponent convertDispenseRequest(
            MedicationOrder.MedicationOrderDispenseRequestComponent orderComponent)
    {
        if (orderComponent == null) {
            return null;
        }

        MedicationRequest.MedicationRequestDispenseRequestComponent requestComponent =
                new MedicationRequest.MedicationRequestDispenseRequestComponent();

        if (orderComponent.hasValidityPeriod()) {
            requestComponent.setValidityPeriod(
                    new Period()
                            .setStart(orderComponent.getValidityPeriod().getStart())
                            .setEnd(orderComponent.getValidityPeriod().getEnd())
            );
        }

        if (orderComponent.hasNumberOfRepeatsAllowed()) {
            requestComponent.setNumberOfRepeatsAllowed(orderComponent.getNumberOfRepeatsAllowed());
        }

        if (orderComponent.hasQuantity()) {
            requestComponent.setQuantity(
                    (SimpleQuantity) new SimpleQuantity()
                            .setValue(orderComponent.getQuantity().getValue())
                            .setUnit(orderComponent.getQuantity().getUnit())
                            .setSystem(orderComponent.getQuantity().getSystem())
                            .setCode(orderComponent.getQuantity().getCode())
            );
        }

        if (orderComponent.hasExpectedSupplyDuration()) {
            requestComponent.setExpectedSupplyDuration(
                    (Duration) new Duration()
                            .setValue(orderComponent.getExpectedSupplyDuration().getValue())
                            .setUnit(orderComponent.getExpectedSupplyDuration().getUnit())
            );
        }

        return requestComponent;
    }
}
