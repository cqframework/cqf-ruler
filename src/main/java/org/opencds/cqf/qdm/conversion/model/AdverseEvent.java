package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Period;

@ResourceDef(name="AdverseEvent", profile="TODO")
public class AdverseEvent extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    private DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public AdverseEvent setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="relevantPeriod", order=1)
    private Period relevantPeriod;
    public Period getRelevantPeriod() {
        return relevantPeriod;
    }
    public AdverseEvent setRelevantPeriod(Period relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }
	
    @Child(name="severity", order=2)
    private Coding severity;
    public Coding getSeverity() {
        return severity;
    }
    public AdverseEvent setSeverity(Coding severity) {
        this.severity = severity;
        return this;
    }

    @Child(name="facilityLocation", order=3)
    private Coding facilityLocation;
    public Coding getFacilityLocation() {
        return facilityLocation;
    }
    public AdverseEvent setFacilityLocation(Coding facilityLocation) {
        this.facilityLocation = facilityLocation;
        return this;
    }	

    @Child(name="type", order=4)
    private Coding type;
    public Coding getType() {
        return type;
    }
    public AdverseEvent setType(Coding type) {
        this.type = type;
        return this;
    }	

    @Override
    public AdverseEvent copy() {
        AdverseEvent retVal = new AdverseEvent();
        super.copyValues(retVal);
        retVal.authorDatetime = authorDatetime;
        retVal.relevantPeriod = relevantPeriod;
        retVal.severity = severity;
        retVal.facilityLocation = facilityLocation;
        retVal.type = type;
        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "AdverseEvent";
    }
}