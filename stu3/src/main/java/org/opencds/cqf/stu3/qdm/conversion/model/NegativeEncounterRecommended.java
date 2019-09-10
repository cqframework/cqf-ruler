package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeEncounterRecommended", profile="TODO")
public class NegativeEncounterRecommended extends EncounterRecommended {
    @Override
    public NegativeEncounterRecommended copy() {
        NegativeEncounterRecommended retVal = new NegativeEncounterRecommended();
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
        return "NegativeEncounterRecommended";
    }
}
