package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveEncounterOrder", profile="TODO")
public class PositiveEncounterOrder extends EncounterOrder {
    @Override
    public PositiveEncounterOrder copy() {
        PositiveEncounterOrder retVal = new PositiveEncounterOrder();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.reason = reason;
        retVal.facilityLocation = facilityLocation;
        retVal.negationRationale = negationRationale;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PositiveEncounterOrder";
    }
}
