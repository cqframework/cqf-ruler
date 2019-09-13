package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Period;

@ResourceDef(name="AllergyIntolerance", profile="TODO")
public class AllergyIntolerance extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    private DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public AllergyIntolerance setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="prevalencePeriod", order=1)
    private Period prevalencePeriod;
    public Period getPrevalencePeriod() {
        return prevalencePeriod;
    }
    public AllergyIntolerance setPrevalencePeriod(Period prevalencePeriod) {
        this.prevalencePeriod = prevalencePeriod;
        return this;
    }	

    @Child(name="type", order=2)
    private Coding type;
    public Coding getType() {
        return type;
    }
    public AllergyIntolerance setType(Coding type) {
        this.type = type;
        return this;
    }

    @Child(name="severity", order=3)
    private Coding severity;
    public Coding getSeverity() {
        return severity;
    }
    public AllergyIntolerance setSeverity(Coding severity) {
        this.severity = severity;
        return this;
    }	

    @Override
    public AllergyIntolerance copy() {
        AllergyIntolerance retVal = new AllergyIntolerance();
        super.copyValues(retVal);
        retVal.authorDatetime = authorDatetime;
        retVal.prevalencePeriod = prevalencePeriod;
        retVal.type = type;
        retVal.severity = severity;
        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "AllergyIntolerance";
    }
}