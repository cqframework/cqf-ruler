package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeAssessmentRecommended", profile="TODO")
public class NegativeAssessmentRecommended extends AssessmentRecommended {
    @Override
    public NegativeAssessmentRecommended copy() {
        NegativeAssessmentRecommended retVal = new NegativeAssessmentRecommended();
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
        return "NegativeAssessmentRecommended";
    }
}
