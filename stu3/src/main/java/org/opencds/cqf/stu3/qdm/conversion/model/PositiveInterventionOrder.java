package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveInterventionOrder", profile="TODO")
public class PositiveInterventionOrder extends InterventionOrder {
    @Override
    public PositiveInterventionOrder copy() {
        PositiveInterventionOrder retVal = new PositiveInterventionOrder();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
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
        return "PositiveInterventionOrder";
    }
}
