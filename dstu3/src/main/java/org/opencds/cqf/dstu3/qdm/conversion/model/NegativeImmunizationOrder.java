package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeImmunizationOrder", profile="TODO")
public class NegativeImmunizationOrder extends ImmunizationOrder {
    @Override
    public NegativeImmunizationOrder copy() {
        NegativeImmunizationOrder retVal = new NegativeImmunizationOrder();
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
        return "NegativeImmunizationOrder";
    }
}
