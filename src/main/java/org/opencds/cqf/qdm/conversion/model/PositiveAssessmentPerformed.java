package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveAssessmentPerformed", profile="TODO")
public class PositiveAssessmentPerformed extends AssessmentPerformed {
    @Override
    public PositiveAssessmentPerformed copy() {
        PositiveAssessmentPerformed retVal = new PositiveAssessmentPerformed();
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
        return "PositiveAssessmentPerformed";
    }
}
