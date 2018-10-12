package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.qdm.QdmBaseType;


@ResourceDef(name="DeviceApplied", profile="TODO")
public abstract class DeviceApplied extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public DeviceApplied setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

	
    @Child(name="relevantPeriod", order=1)
    Interval relevantPeriod;
    public Interval getRelevantPeriod() {
        return relevantPeriod;
    }
    public DeviceApplied setRelevantPeriod(Interval relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	
	
	
	
    @Child(name="negationRationale", order=2)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public DeviceApplied setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

	
    @Child(name="reason", order=3)
    Code reason;
    public Code getReason() {
        return reason;
    }
    public DeviceApplied setReason(Code reason) {
        this.reason = reason;
        return this;
    }


    @Child(name="anatomicalLocationSite", order=4)
    Code anatomicalLocationSite;
    public Code getAnatomicalLocationSite() {
        return anatomicalLocationSite;
    }
    public DeviceApplied setAnatomicalLocationSite(Code anatomicalLocationSite) {
        this.anatomicalLocationSite = anatomicalLocationSite;
        return this;
    }


    @Child(name="anatomicalApproachSite", order=5)
    Code anatomicalApproachSite;
    public Code getAnatomicalApproachSite() {
        return anatomicalApproachSite;
    }
    public DeviceApplied setAnatomicalApproachSite(Code anatomicalApproachSite) {
        this.anatomicalApproachSite = anatomicalApproachSite;
        return this;
    }	
	
}
