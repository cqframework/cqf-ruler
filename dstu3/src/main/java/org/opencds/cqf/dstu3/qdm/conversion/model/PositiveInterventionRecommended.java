package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveInterventionRecommended", profile="TODO")
public class PositiveInterventionRecommended extends InterventionRecommended {
    @Override
    public PositiveInterventionRecommended copy() {
        PositiveInterventionRecommended retVal = new PositiveInterventionRecommended();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.reason = reason;
        retVal.negationRationale = negationRationale;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PositiveInterventionRecommended";
    }
}
