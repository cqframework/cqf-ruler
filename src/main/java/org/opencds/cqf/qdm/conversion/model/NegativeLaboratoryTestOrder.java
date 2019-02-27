package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeLaboratoryTestOrder", profile="TODO")
public class NegativeLaboratoryTestOrder extends LaboratoryTestOrder {
    @Override
    public NegativeLaboratoryTestOrder copy() {
        NegativeLaboratoryTestOrder retVal = new NegativeLaboratoryTestOrder();
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
        return "NegativeLaboratoryTestOrder";
    }
}
