package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveSubstanceAdministered", profile="TODO")
public class PositiveSubstanceAdministered extends SubstanceAdministered {
    @Override
    public PositiveSubstanceAdministered copy() {
        PositiveSubstanceAdministered retVal = new PositiveSubstanceAdministered();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.relevantPeriod = relevantPeriod;
        retVal.dosage = dosage;
        retVal.supply = supply;
        retVal.frequency = frequency;
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
        return "PositiveSubstanceAdministered";
    }
}
