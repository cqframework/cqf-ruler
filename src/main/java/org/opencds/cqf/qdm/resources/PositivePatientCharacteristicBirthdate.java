package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositivePatientCharacteristicBirthdate", profile="TODO")
public class PositivePatientCharacteristicBirthdate extends PatientCharacteristicBirthdate {
    @Override
    public PositivePatientCharacteristicBirthdate copy() {
        PositivePatientCharacteristicBirthdate retVal = new PositivePatientCharacteristicBirthdate();
        super.copyValues(retVal);

        retVal.birthDatetime = birthDatetime;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PositivePatientCharacteristicBirthdate";
    }

    
}
