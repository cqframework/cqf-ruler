package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Coding;

@ResourceDef(name="Symptom", profile="TODO")
public class Symptom extends QdmBaseType {

    @Child(name="prevalencePeriod", order=0)
    private Period prevalencePeriod;
    public Period getPrevalencePeriod() {
        return prevalencePeriod;
    }
    public Symptom setPrevalencePeriod(Period prevalencePeriod) {
        this.prevalencePeriod = prevalencePeriod;
        return this;
    }	

    @Child(name="severity", order=1)
    private Coding severity;
    public Coding getSeverity() {
        return severity;
    }
    public Symptom setSeverity(Coding severity) {
        this.severity = severity;
        return this;
    }	

    @Override
    public Symptom copy() {
        Symptom retVal = new Symptom();
        super.copyValues(retVal);
        retVal.prevalencePeriod = prevalencePeriod;
        retVal.severity = severity;
        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "Symptom";
    }
}