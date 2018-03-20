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

        try {
            switch (resource.getResourceType().name()) {
                case "MedicationOrder":
                    return resolveMedicationRequest((MedicationOrder) resource, converter);
                default:
                    return converter.convertResource(resource);
            }
        } catch (FHIRException fe) {
            fe.printStackTrace();
            throw new RuntimeException("Error converting type: " + resource.getResourceType().name() + "\nMessage: " + fe.getMessage());
        }
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
                        .setStatus(MedicationRequest.MedicationRequestStatus.fromCode(order.getStatus().toCode()))
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
