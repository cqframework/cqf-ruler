package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveImmunizationOrder", profile="TODO")
public class PositiveImmunizationOrder extends ImmunizationOrder {
    @Override
    public PositiveImmunizationOrder copy() {
        PositiveImmunizationOrder retVal = new PositiveImmunizationOrder();
        super.copyValues(retVal);

        retVal.activeDatetime = activeDatetime;
        retVal.authorDatetime = authorDatetime;
        retVal.dosage = dosage;
        retVal.supply = supply;
        retVal.reason = reason;
        retVal.route = route;
        retVal.negationRationale = negationRationale;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PositiveImmunizationOrder";
    }
}
