package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositivePatientCharacteristicRace", profile="TODO")
public class PositivePatientCharacteristicRace extends PatientCharacteristicRace {
    @Override
    public PositivePatientCharacteristicRace copy() {
        PositivePatientCharacteristicRace retVal = new PositivePatientCharacteristicRace();
        super.copyValues(retVal);

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PositivePatientCharacteristicRace";
    }
}
