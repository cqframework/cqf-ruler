package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeInterventionRecommended", profile="TODO")
public class NegativeInterventionRecommended extends InterventionRecommended {
    @Override
    public NegativeInterventionRecommended copy() {
        NegativeInterventionRecommended retVal = new NegativeInterventionRecommended();
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
        return "NegativeInterventionRecommended";
    }
}
