package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeDeviceOrder", profile="TODO")
public class NegativeDeviceOrder extends DeviceOrder {
    @Override
    public NegativeDeviceOrder copy() {
        NegativeDeviceOrder retVal = new NegativeDeviceOrder();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.negationRationale = negationRationale;
        retVal.reason = reason;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "NegativeDeviceOrder";
    }
}
