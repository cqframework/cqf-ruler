package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveMedicationDischarge", profile="TODO")
public class PositiveMedicationDischarge extends MedicationDischarge {
    @Override
    public PositiveMedicationDischarge copy() {
        PositiveMedicationDischarge retVal = new PositiveMedicationDischarge();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.refills = refills;
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
        return "PositiveMedicationDischarge";
    }
}
