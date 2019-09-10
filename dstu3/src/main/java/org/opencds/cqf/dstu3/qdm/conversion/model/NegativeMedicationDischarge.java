package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeMedicationDischarge", profile="TODO")
public class NegativeMedicationDischarge extends MedicationDischarge {
    @Override
    public NegativeMedicationDischarge copy() {
        NegativeMedicationDischarge retVal = new NegativeMedicationDischarge();
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
        return "NegativeMedicationDischarge";
    }
}
