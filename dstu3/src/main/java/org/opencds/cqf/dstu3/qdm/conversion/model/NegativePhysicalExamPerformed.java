package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativePhysicalExamPerformed", profile="TODO")
public class NegativePhysicalExamPerformed extends PhysicalExamPerformed {
    @Override
    public NegativePhysicalExamPerformed copy() {
        NegativePhysicalExamPerformed retVal = new NegativePhysicalExamPerformed();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.relevantPeriod = relevantPeriod;
        retVal.reason = reason;
        retVal.method = method;
        retVal.result = result;
        retVal.anatomicalLocationSite = anatomicalLocationSite;
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
        return "NegativePhysicalExamPerformed";
    }
}
