package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveLaboratoryTestRecommended", profile="TODO")
public class PositiveLaboratoryTestRecommended extends LaboratoryTestRecommended {
    @Override
    public PositiveLaboratoryTestRecommended copy() {
        PositiveLaboratoryTestRecommended retVal = new PositiveLaboratoryTestRecommended();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.method = method;
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
        return "PositiveLaboratoryTestRecommended";
    }
}
