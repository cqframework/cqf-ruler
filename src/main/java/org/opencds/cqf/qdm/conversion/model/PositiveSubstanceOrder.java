package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveSubstanceOrder", profile="TODO")
public class PositiveSubstanceOrder extends SubstanceOrder {
    @Override
    public PositiveSubstanceOrder copy() {
        PositiveSubstanceOrder retVal = new PositiveSubstanceOrder();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.reason = reason;
        retVal.dosage = dosage;
        retVal.supply = supply;
        retVal.frequency = frequency;
        retVal.method = method;
        retVal.refills = refills;
        retVal.route = route;
        retVal.negationRationale = negationRationale;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PositiveSubstanceOrder";
    }
}
