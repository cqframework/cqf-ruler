package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeImmunizationAdministered", profile="TODO")
public class NegativeImmunizationAdministered extends ImmunizationAdministered {
    @Override
    public NegativeImmunizationAdministered copy() {
        NegativeImmunizationAdministered retVal = new NegativeImmunizationAdministered();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.reason = reason;
        retVal.dosage = dosage;
        retVal.supply = supply;
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
        return "NegativeImmunizationAdministered";
    }
}
