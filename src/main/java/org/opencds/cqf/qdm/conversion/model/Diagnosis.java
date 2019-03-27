package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="Diagnosis", profile="TODO")
public class Diagnosis extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public Diagnosis setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }
	
    @Child(name="prevalencePeriod", order=1)
    Period prevalencePeriod;
    public Period getPrevalencePeriod() {
        return prevalencePeriod;
    }
    public Diagnosis setPrevalencePeriod(Period prevalencePeriod) {
        this.prevalencePeriod = prevalencePeriod;
        return this;
    }

    @Child(name="reason", order=2)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public Diagnosis setReason(Coding reason) {
        this.reason = reason;
        return this;
    }
	
    @Child(name="anatomicalLocationSite", order=3)
    Coding anatomicalLocationSite;
    public Coding getAnatomicalLocationSite() {
        return anatomicalLocationSite;
    }
    public Diagnosis setAnatomicalLocationSite(Coding anatomicalLocationSite) {
        this.anatomicalLocationSite = anatomicalLocationSite;
        return this;
    }

    @Child(name="severity", order=4)
    Coding severity;
    public Coding getSeverity() {
        return severity;
    }
    public Diagnosis setSeverity(Coding severity) {
        this.severity = severity;
        return this;
    }

    @Override
    public QdmBaseType copy() {
        Diagnosis retVal = new Diagnosis();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.prevalencePeriod = prevalencePeriod;
        retVal.reason = reason;
        retVal.anatomicalLocationSite = anatomicalLocationSite;
        retVal.severity = severity;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "Diagnosis";
    }
}
