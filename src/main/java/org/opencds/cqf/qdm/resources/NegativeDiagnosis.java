package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeDiagnosis", profile="TODO")
public class NegativeDiagnosis extends Diagnosis {
    @Override
    public NegativeDiagnosis copy() {
        NegativeDiagnosis retVal = new NegativeDiagnosis();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.prevalencePeriod = prevalencePeriod;
        retVal.anatomicalLocationSite = anatomicalLocationSite;
        retVal.severity = severity;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "NegativeDiagnosis";
    }
}
