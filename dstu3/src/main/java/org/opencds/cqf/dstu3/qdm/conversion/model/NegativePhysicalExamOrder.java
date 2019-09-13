package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativePhysicalExamOrder", profile="TODO")
public class NegativePhysicalExamOrder extends PhysicalExamOrder {
    @Override
    public NegativePhysicalExamOrder copy() {
        NegativePhysicalExamOrder retVal = new NegativePhysicalExamOrder();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.reason = reason;
        retVal.method = method;
        retVal.anatomicalLocationSite = anatomicalLocationSite;
        retVal.negationRationale = negationRationale;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "NegativePhysicalExamOrder";
    }
}
