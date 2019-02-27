package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveProcedurePerformed", profile="TODO")
public class PositiveProcedurePerformed extends ProcedurePerformed {
    @Override
    public PositiveProcedurePerformed copy() {
        PositiveProcedurePerformed retVal = new PositiveProcedurePerformed();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.relevantPeriod = relevantPeriod;
        retVal.reason = reason;
        retVal.method = method;
        retVal.result = result;
        retVal.status = status;
        retVal.anatomicalApproachSite = anatomicalApproachSite;
        retVal.ordinality = ordinality;
        retVal.incisionDatetime = incisionDatetime;
        retVal.negationRationale = negationRationale;
        retVal.components = components;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PositiveProcedurePerformed";
    }

    
}
