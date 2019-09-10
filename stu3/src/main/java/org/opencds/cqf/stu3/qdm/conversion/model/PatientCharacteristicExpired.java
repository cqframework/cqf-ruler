package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Coding;

@ResourceDef(name="PatientCharacteristicExpired", profile="TODO")
public class PatientCharacteristicExpired extends QdmBaseType {

    @Child(name="expiredDatetime", order=0)
    private DateTimeType expiredDatetime;
    public DateTimeType getExpiredDatetime() {
        return expiredDatetime;
    }
    public PatientCharacteristicExpired setExpiredDatetime(DateTimeType expiredDatetime) {
        this.expiredDatetime = expiredDatetime;
        return this;
    }

    @Child(name="cause", order=1)
    private Coding cause;
    public Coding getCause() {
        return cause;
    }
    public PatientCharacteristicExpired setCause(Coding cause) {
        this.cause = cause;
        return this;
    }

    @Override
    public PatientCharacteristicExpired copy() {
        PatientCharacteristicExpired retVal = new PatientCharacteristicExpired();
        super.copyValues(retVal);
        retVal.expiredDatetime = expiredDatetime;
        retVal.cause = cause;
        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PatientCharacteristicExpired";
    }
}