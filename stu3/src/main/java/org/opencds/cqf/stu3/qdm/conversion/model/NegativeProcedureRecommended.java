package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeProcedureRecommended", profile="TODO")
public class NegativeProcedureRecommended extends ProcedureRecommended {
    @Override
    public NegativeProcedureRecommended copy() {
        NegativeProcedureRecommended retVal = new NegativeProcedureRecommended();
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
        return "NegativeProcedureRecommended";
    }
}
