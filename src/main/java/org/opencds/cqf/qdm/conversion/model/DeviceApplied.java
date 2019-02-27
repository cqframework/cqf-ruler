package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Period;

@ResourceDef(name="DeviceApplied", profile="TODO")
public abstract class DeviceApplied extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public DeviceApplied setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }
	
    @Child(name="relevantPeriod", order=1)
    Period relevantPeriod;
    public Period getRelevantPeriod() {
        return relevantPeriod;
    }
    public DeviceApplied setRelevantPeriod(Period relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }
	
    @Child(name="negationRationale", order=2)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public DeviceApplied setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

    @Child(name="reason", order=3)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public DeviceApplied setReason(Coding reason) {
        this.reason = reason;
        return this;
    }

    @Child(name="anatomicalLocationSite", order=4)
    Coding anatomicalLocationSite;
    public Coding getAnatomicalLocationSite() {
        return anatomicalLocationSite;
    }
    public DeviceApplied setAnatomicalLocationSite(Coding anatomicalLocationSite) {
        this.anatomicalLocationSite = anatomicalLocationSite;
        return this;
    }

    @Child(name="anatomicalApproachSite", order=5)
    Coding anatomicalApproachSite;
    public Coding getAnatomicalApproachSite() {
        return anatomicalApproachSite;
    }
    public DeviceApplied setAnatomicalApproachSite(Coding anatomicalApproachSite) {
        this.anatomicalApproachSite = anatomicalApproachSite;
        return this;
    }
}
