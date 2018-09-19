package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.qdm.QdmBaseType;
import org.opencds.cqf.cql.runtime.DateTime;


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
