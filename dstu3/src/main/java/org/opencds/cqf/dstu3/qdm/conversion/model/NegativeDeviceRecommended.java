package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeDeviceRecommended", profile="TODO")
public class NegativeDeviceRecommended extends DeviceRecommended {
    @Override
    public NegativeDeviceRecommended copy() {
        NegativeDeviceRecommended retVal = new NegativeDeviceRecommended();
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
        return "NegativeDeviceRecommended";
    }
}
