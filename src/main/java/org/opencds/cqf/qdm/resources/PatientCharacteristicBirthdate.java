package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.opencds.cqf.cql.runtime.DateTime;

import java.util.List;

@ResourceDef(name="PatientCharacteristicBirthdate", profile="TODO")
public abstract class PatientCharacteristicBirthdate extends QdmBaseType {

    @Child(name="birthDatetime", order=0)
    DateTime birthDatetime;
    public DateTime getBirthDatetime() {
        return birthDatetime;
    }
    public PatientCharacteristicBirthdate setBirthDatetime(DateTime birthDatetime) {
        this.birthDatetime = birthDatetime;
        return this;
    }	
	

}
