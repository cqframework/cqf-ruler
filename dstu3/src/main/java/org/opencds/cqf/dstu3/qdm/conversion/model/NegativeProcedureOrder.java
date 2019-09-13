package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeProcedureOrder", profile="TODO")
public class NegativeProcedureOrder extends ProcedureOrder {
    @Override
    public NegativeProcedureOrder copy() {
        NegativeProcedureOrder retVal = new NegativeProcedureOrder();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.reason = reason;
        retVal.method = method;
        retVal.anatomicalApproachSite = anatomicalApproachSite;
        retVal.anatomicalLocationSite = anatomicalLocationSite;
        retVal.ordinality = ordinality;
        retVal.negationRationale = negationRationale;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "NegativeProcedureOrder";
    }
}
