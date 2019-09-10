package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveLaboratoryTestOrder", profile="TODO")
public class PositiveLaboratoryTestOrder extends LaboratoryTestOrder {
    @Override
    public PositiveLaboratoryTestOrder copy() {
        PositiveLaboratoryTestOrder retVal = new PositiveLaboratoryTestOrder();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.reason = reason;
        retVal.method = method;
        retVal.negationRationale = negationRationale;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PositiveLaboratoryTestOrder";
    }
}
