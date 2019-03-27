package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveLaboratoryTestPerformed", profile="TODO")
public class PositiveLaboratoryTestPerformed extends LaboratoryTestPerformed {
    @Override
    public PositiveLaboratoryTestPerformed copy() {
        PositiveLaboratoryTestPerformed retVal = new PositiveLaboratoryTestPerformed();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.relevantPeriod = relevantPeriod;
        retVal.status = status;
        retVal.method = method;
        retVal.result = result;
        retVal.resultDatetime = resultDatetime;
        retVal.reason = reason;
        retVal.referenceRange = referenceRange;
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
        return "PositiveLaboratoryTestPerformed";
    }
}
