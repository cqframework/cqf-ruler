package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveEncounterRecommended", profile="TODO")
public class PositiveEncounterRecommended extends EncounterRecommended {
    @Override
    public PositiveEncounterRecommended copy() {
        PositiveEncounterRecommended retVal = new PositiveEncounterRecommended();
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
        return "PositiveEncounterRecommended";
    }
}
