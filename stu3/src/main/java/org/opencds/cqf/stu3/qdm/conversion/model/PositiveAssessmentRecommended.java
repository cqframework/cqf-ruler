package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveAssessmentRecommended", profile="TODO")
public class PositiveAssessmentRecommended extends AssessmentRecommended {
    @Override
    public PositiveAssessmentRecommended copy() {
        PositiveAssessmentRecommended retVal = new PositiveAssessmentRecommended();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.negationRationale = negationRationale;
        retVal.reason = reason;
        retVal.method = method;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PositiveAssessmentRecommended";
    }
}
