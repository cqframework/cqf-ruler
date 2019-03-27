package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeDiagnosticStudyPerformed", profile="TODO")
public class NegativeDiagnosticStudyPerformed extends DiagnosticStudyPerformed {
    @Override
    public NegativeDiagnosticStudyPerformed copy() {
        NegativeDiagnosticStudyPerformed retVal = new NegativeDiagnosticStudyPerformed();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.relevantPeriod = relevantPeriod;
        retVal.reason = reason;
        retVal.result = result;
        retVal.resultDatetime = resultDatetime;
        retVal.status = status;
        retVal.method = method;
        retVal.facilityLocation = facilityLocation;
        retVal.negationRationale = negationRationale;
        retVal.components = components;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "NegativeDiagnosticStudyPerformed";
    }
}
