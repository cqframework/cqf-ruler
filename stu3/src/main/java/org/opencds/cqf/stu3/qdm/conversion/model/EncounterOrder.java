package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;

@ResourceDef(name="EncounterOrder", profile="TODO")
public abstract class EncounterOrder extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public EncounterOrder setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="reason", order=1)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public EncounterOrder setReason(Coding reason) {
        this.reason = reason;
        return this;
    }

    @Child(name="facilityLocation", order=2)
    Coding facilityLocation;
    public Coding getFacilityLocation() {
        return facilityLocation;
    }
    public EncounterOrder setFacilityLocation(Coding facilityLocation) {
        this.facilityLocation = facilityLocation;
        return this;
    }

    @Child(name="negationRationale", order=3)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public EncounterOrder setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
