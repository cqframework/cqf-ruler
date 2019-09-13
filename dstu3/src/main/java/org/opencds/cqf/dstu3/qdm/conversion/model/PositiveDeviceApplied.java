package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveDeviceApplied", profile="TODO")
public class PositiveDeviceApplied extends DeviceApplied {
    @Override
    public PositiveDeviceApplied copy() {
        PositiveDeviceApplied retVal = new PositiveDeviceApplied();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.relevantPeriod = relevantPeriod;
        retVal.negationRationale = negationRationale;
        retVal.reason = reason;
        retVal.anatomicalLocationSite = anatomicalLocationSite;
        retVal.anatomicalApproachSite = anatomicalApproachSite;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PositiveDeviceApplied";
    }
}
