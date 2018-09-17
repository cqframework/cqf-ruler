package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositivePatientCharacteristicEthnicity", profile="TODO")
public class PositivePatientCharacteristicEthnicity extends PatientCharacteristicEthnicity {
    @Override
    public PositivePatientCharacteristicEthnicity copy() {
        PositivePatientCharacteristicEthnicity retVal = new PositivePatientCharacteristicEthnicity();
        super.copyValues(retVal);

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PositivePatientCharacteristicEthnicity";
    }
}
