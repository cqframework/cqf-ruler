package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeLaboratoryTestRecommended", profile="TODO")
public class NegativeLaboratoryTestRecommended extends LaboratoryTestRecommended {
    @Override
    public NegativeLaboratoryTestRecommended copy() {
        NegativeLaboratoryTestRecommended retVal = new NegativeLaboratoryTestRecommended();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.method = method;
        retVal.reason = reason;
        retVal.negationRationale = negationRationale;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "NegativeLaboratoryTestRecommended";
    }
}
