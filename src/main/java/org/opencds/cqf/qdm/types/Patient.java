package org.opencds.cqf.qdm.types;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.cql.runtime.DateTime;

@DatatypeDef(name="Patient")
public class Patient extends Type implements ICompositeType {

    @Child(name = "birthDatetime", order = 0)
    private DateTime birthDatetime;
    public DateTime getBirthDatetime() {
        return birthDatetime;
    }
    public Patient setBirthDatetime(DateTime birthDatetime) {
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
