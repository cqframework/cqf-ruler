package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;

@ResourceDef(name="DeviceRecommended", profile="TODO")
public abstract class DeviceRecommended extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public DeviceRecommended setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }
	
    @Child(name="negationRationale", order=1)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public DeviceRecommended setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

    @Child(name="reason", order=2)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public DeviceRecommended setReason(Coding reason) {
        this.reason = reason;
        return this;
    }
}
