package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositivePatientCharacteristicSex", profile="TODO")
public class PositivePatientCharacteristicSex extends PatientCharacteristicSex {
    @Override
    public PositivePatientCharacteristicSex copy() {
        PositivePatientCharacteristicSex retVal = new PositivePatientCharacteristicSex();
        super.copyValues(retVal);

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PositivePatientCharacteristicSex";
    }
}
