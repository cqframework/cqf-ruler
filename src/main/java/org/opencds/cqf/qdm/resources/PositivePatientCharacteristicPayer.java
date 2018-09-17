package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositivePatientCharacteristicPayer", profile="TODO")
public class PositivePatientCharacteristicPayer extends PatientCharacteristicPayer {
    @Override
    public PositivePatientCharacteristicPayer copy() {
        PositivePatientCharacteristicPayer retVal = new PositivePatientCharacteristicPayer();
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
        return "PositivePatientCharacteristicPayer";
    }

    
}
