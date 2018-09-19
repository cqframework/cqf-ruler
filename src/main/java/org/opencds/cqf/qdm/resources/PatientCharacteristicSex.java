package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.opencds.cqf.qdm.QdmBaseType;


@ResourceDef(name="PatientCharacteristicSex", profile="TODO")
public class PatientCharacteristicSex extends QdmBaseType {
	
    @Override
    public PatientCharacteristicSex copy() {
        PatientCharacteristicSex retVal = new PatientCharacteristicSex();
        super.copyValues(retVal);

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PatientCharacteristicSex";
    }

}
