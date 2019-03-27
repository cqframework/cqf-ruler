package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveDeviceRecommended", profile="TODO")
public class PositiveDeviceRecommended extends DeviceRecommended {
    @Override
    public PositiveDeviceRecommended copy() {
        PositiveDeviceRecommended retVal = new PositiveDeviceRecommended();
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
        return "PositiveDeviceRecommended";
    }
}
