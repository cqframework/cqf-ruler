package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeAssessmentPerformed", profile="TODO")
public class NegativeAssessmentPerformed extends AssessmentPerformed {
    @Override
    public NegativeAssessmentPerformed copy() {
        NegativeAssessmentPerformed retVal = new NegativeAssessmentPerformed();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.negationRationale = negationRationale;
        retVal.reason = reason;
        retVal.method = method;
        retVal.result = result;
        retVal.components = components;
        retVal.relatedTo = relatedTo;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "NegativeAssessmentPerformed";
    }
}
