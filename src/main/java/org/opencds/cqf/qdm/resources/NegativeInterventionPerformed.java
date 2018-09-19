package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeInterventionPerformed", profile="TODO")
public class NegativeInterventionPerformed extends InterventionPerformed {
    @Override
    public NegativeInterventionPerformed copy() {
        NegativeInterventionPerformed retVal = new NegativeInterventionPerformed();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.relevantPeriod = relevantPeriod;
        retVal.reason = reason;
        retVal.status = status;
        retVal.negationRationale = negationRationale;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "NegativeInterventionPerformed";
    }
}
