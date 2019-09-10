package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PatientCharacteristicRace", profile="TODO")
public class PatientCharacteristicRace extends QdmBaseType {
	
   @Override
    public PatientCharacteristicRace copy() {
        PatientCharacteristicRace retVal = new PatientCharacteristicRace();
        super.copyValues(retVal);

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PatientCharacteristicRace";
    }
}
