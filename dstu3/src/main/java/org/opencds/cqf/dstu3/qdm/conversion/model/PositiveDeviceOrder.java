package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveDeviceOrder", profile="TODO")
public class PositiveDeviceOrder extends DeviceOrder {
    @Override
    public PositiveDeviceOrder copy() {
        PositiveDeviceOrder retVal = new PositiveDeviceOrder();
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
        return "PositiveDeviceOrder";
    }
}
