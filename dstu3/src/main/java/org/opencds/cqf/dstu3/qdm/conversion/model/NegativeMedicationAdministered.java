package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeMedicationAdministered", profile="TODO")
public class NegativeMedicationAdministered extends MedicationAdministered {
    @Override
    public NegativeMedicationAdministered copy() {
        NegativeMedicationAdministered retVal = new NegativeMedicationAdministered();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.relevantPeriod = relevantPeriod;
        retVal.dosage = dosage;
        retVal.supply = supply;
        retVal.frequency = frequency;
        retVal.route = route;
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
        return "NegativeMedicationAdministered";
    }
}
