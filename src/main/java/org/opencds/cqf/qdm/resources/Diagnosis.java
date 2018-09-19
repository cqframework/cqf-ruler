package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.qdm.QdmBaseType;


@ResourceDef(name="Diagnosis", profile="TODO")
public abstract class Diagnosis extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public Diagnosis setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

	
    @Child(name="prevalencePeriod", order=1)
    Interval prevalencePeriod;
    public Interval getPrevalencePeriod() {
        return prevalencePeriod;
    }
    public Diagnosis setPrevalencePeriod(Interval prevalencePeriod) {
        this.prevalencePeriod = prevalencePeriod;
        return this;
    }	
	

    @Child(name="reason", order=2)
    Code reason;
    public Code getReason() {
        return reason;
    }
    public Diagnosis setReason(Code reason) {
        this.reason = reason;
        return this;
    }

	
    @Child(name="anatomicalLocationSite", order=3)
    Code anatomicalLocationSite;
    public Code getAnatomicalLocationSite() {
        return anatomicalLocationSite;
    }
    public Diagnosis setAnatomicalLocationSite(Code anatomicalLocationSite) {
        this.anatomicalLocationSite = anatomicalLocationSite;
        return this;
    }


    @Child(name="severity", order=4)
    Code severity;
    public Code getSeverity() {
        return severity;
    }
    public Diagnosis setSeverity(Code severity) {
        this.severity = severity;
        return this;
    }
	


}
