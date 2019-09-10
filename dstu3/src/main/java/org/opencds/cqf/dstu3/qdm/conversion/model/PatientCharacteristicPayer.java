package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Period;

@ResourceDef(name="PatientCharacteristicPayer", profile="TODO")
public class PatientCharacteristicPayer extends QdmBaseType {

    @Child(name="relevantPeriod", order=0)
    private Period relevantPeriod;
    public Period getRelevantPeriod() {
        return relevantPeriod;
    }
    public PatientCharacteristicPayer setRelevantPeriod(Period relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	

    @Override
    public PatientCharacteristicPayer copy() {
        PatientCharacteristicPayer retVal = new PatientCharacteristicPayer();
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
        return "PatientCharacteristicPayer";
    }
}
