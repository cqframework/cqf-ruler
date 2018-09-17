package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativePatientCharacteristicPayer", profile="TODO")
public class NegativePatientCharacteristicPayer extends PatientCharacteristicPayer {
    @Override
    public NegativePatientCharacteristicPayer copy() {
        NegativePatientCharacteristicPayer retVal = new NegativePatientCharacteristicPayer();
        super.copyValues(retVal);

        retVal.relevantPeriod = relevantPeriod;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "NegativePatientCharacteristicPayer";
    }

    
}
