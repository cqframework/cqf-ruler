package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeDiagnosticStudyOrder", profile="TODO")
public class NegativeDiagnosticStudyOrder extends DiagnosticStudyOrder {
    @Override
    public NegativeDiagnosticStudyOrder copy() {
        NegativeDiagnosticStudyOrder retVal = new NegativeDiagnosticStudyOrder();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.reason = reason;
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
        return "NegativeDiagnosticStudyOrder";
    }
}
