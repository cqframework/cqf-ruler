package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveDiagnosticStudyRecommended", profile="TODO")
public class PositiveDiagnosticStudyRecommended extends DiagnosticStudyRecommended {
    @Override
    public PositiveDiagnosticStudyRecommended copy() {
        PositiveDiagnosticStudyRecommended retVal = new PositiveDiagnosticStudyRecommended();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.method = method;
        retVal.negationRationale = negationRationale;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PositiveDiagnosticStudyRecommended";
    }
}
