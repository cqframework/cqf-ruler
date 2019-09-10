package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveDiagnosticStudyOrder", profile="TODO")
public class PositiveDiagnosticStudyOrder extends DiagnosticStudyOrder {
    @Override
    public PositiveDiagnosticStudyOrder copy() {
        PositiveDiagnosticStudyOrder retVal = new PositiveDiagnosticStudyOrder();
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
        return "PositiveDiagnosticStudyOrder";
    }
}
