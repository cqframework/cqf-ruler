package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeDiagnosticStudyRecommended", profile="TODO")
public class NegativeDiagnosticStudyRecommended extends DiagnosticStudyRecommended {
    @Override
    public NegativeDiagnosticStudyRecommended copy() {
        NegativeDiagnosticStudyRecommended retVal = new NegativeDiagnosticStudyRecommended();
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
        return "NegativeDiagnosticStudyRecommended";
    }
}
