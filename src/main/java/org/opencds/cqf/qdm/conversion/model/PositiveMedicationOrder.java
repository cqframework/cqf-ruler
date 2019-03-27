package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveMedicationOrder", profile="TODO")
public class PositiveMedicationOrder extends MedicationOrder {
    @Override
    public PositiveMedicationOrder copy() {
        PositiveMedicationOrder retVal = new PositiveMedicationOrder();
        super.copyValues(retVal);

        retVal.activeDatetime = activeDatetime;
        retVal.relevantPeriod = relevantPeriod;
        retVal.authorDatetime = authorDatetime;
        retVal.refills = refills;
        retVal.dosage = dosage;
        retVal.supply = supply;
        retVal.frequency = frequency;
        retVal.route = route;
        retVal.method = method;
        retVal.reason = reason;
        retVal.negationRationale = negationRationale;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PositiveMedicationOrder";
    }
}
