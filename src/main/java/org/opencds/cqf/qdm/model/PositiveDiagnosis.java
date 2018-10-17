package org.opencds.cqf.qdm.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveDiagnosis", profile="TODO")
public class PositiveDiagnosis extends Diagnosis {
    @Override
    public PositiveDiagnosis copy() {
        PositiveDiagnosis retVal = new PositiveDiagnosis();
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
        return "PositiveDiagnosis";
    }
}
