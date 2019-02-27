package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveProcedureRecommended", profile="TODO")
public class PositiveProcedureRecommended extends ProcedureRecommended {
    @Override
    public PositiveProcedureRecommended copy() {
        PositiveProcedureRecommended retVal = new PositiveProcedureRecommended();
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
        return "PositiveProcedureRecommended";
    }
}
