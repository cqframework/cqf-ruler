package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveInterventionPerformed", profile="TODO")
public class PositiveInterventionPerformed extends InterventionPerformed {
    @Override
    public PositiveInterventionPerformed copy() {
        PositiveInterventionPerformed retVal = new PositiveInterventionPerformed();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.relevantPeriod = relevantPeriod;
        retVal.reason = reason;
        retVal.result = result;
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
        return "PositiveInterventionPerformed";
    }
}
