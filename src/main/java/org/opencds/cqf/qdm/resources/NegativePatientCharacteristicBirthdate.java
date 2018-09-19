package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativePatientCharacteristicBirthdate", profile="TODO")
public class NegativePatientCharacteristicBirthdate extends PatientCharacteristicBirthdate {
    @Override
    public NegativePatientCharacteristicBirthdate copy() {
        NegativePatientCharacteristicBirthdate retVal = new NegativePatientCharacteristicBirthdate();
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
        return "NegativePatientCharacteristicBirthdate";
    }

    
}
