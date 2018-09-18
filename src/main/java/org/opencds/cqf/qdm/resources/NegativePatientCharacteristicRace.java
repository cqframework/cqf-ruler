package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativePatientCharacteristicRace", profile="TODO")
public class NegativePatientCharacteristicRace extends PatientCharacteristicRace {
    @Override
    public NegativePatientCharacteristicRace copy() {
        NegativePatientCharacteristicRace retVal = new NegativePatientCharacteristicRace();
        super.copyValues(retVal);

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "NegativePatientCharacteristicRace";
    }
}
