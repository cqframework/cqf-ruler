package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.ICompositeType;

@DatatypeDef(name="Patient")
public class Patient extends Type implements ICompositeType {

    @Child(name = "birthDatetime", order = 0)
    private DateTimeType birthDatetime;
    public DateTimeType getBirthDatetime() {
        return birthDatetime;
    }
    public Patient setBirthDatetime(DateTimeType birthDatetime) {
        this.birthDatetime = birthDatetime;
        return this;
    }

    @Override
    protected Patient typedCopy() {
        Patient retVal = new Patient();
        super.copyValues(retVal);
        retVal.birthDatetime = birthDatetime;
        return retVal;
    }
}
