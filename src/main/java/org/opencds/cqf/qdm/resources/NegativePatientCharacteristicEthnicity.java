package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativePatientCharacteristicEthnicity", profile="TODO")
public class NegativePatientCharacteristicEthnicity extends PatientCharacteristicEthnicity {
    @Override
    public NegativePatientCharacteristicEthnicity copy() {
        NegativePatientCharacteristicEthnicity retVal = new NegativePatientCharacteristicEthnicity();
        super.copyValues(retVal);

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "NegativePatientCharacteristicEthnicity";
    }
}
