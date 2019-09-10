package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Type;

import java.util.List;

@ResourceDef(name="PhysicalExamPerformed", profile="TODO")
public abstract class PhysicalExamPerformed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public PhysicalExamPerformed setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="relevantPeriod", order=1)
    Period relevantPeriod;
    public Period getRelevantPeriod() {
        return relevantPeriod;
    }
    public PhysicalExamPerformed setRelevantPeriod(Period relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }
	
	@Child(name="reason", order=2)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public PhysicalExamPerformed setReason(Coding reason) {
        this.reason = reason;
        return this;
    }
	
    @Child(name="method", order=3)
    Coding method;
    public Coding getMethod() {
        return method;
    }
    public PhysicalExamPerformed setMethod(Coding method) {
        this.method = method;
        return this;
    }

    @Child(name="result", order=4)
    Type result;
    public Type getResult() {
        return result;
    }
    public PhysicalExamPerformed setResult(Type result) {
        this.result = result;
        return this;
    }

    @Child(name="anatomicalLocationSite", order=5)
    Coding anatomicalLocationSite;
    public Coding getanAtomicalLocationSite() {
        return anatomicalLocationSite;
    }
    public PhysicalExamPerformed setAnatomicalLocationSite(Coding anatomicalLocationSite) {
        this.anatomicalLocationSite = anatomicalLocationSite;
        return this;
    }	

    @Child(name="negationRationale", order=6)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public PhysicalExamPerformed setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

    @Child(name="components", max=Child.MAX_UNLIMITED, order=7)
    List<Component> components;
    public List<Component> getComponents() {
        return components;
    }
    public PhysicalExamPerformed setComponents(List<Component> components) {
        this.components = components;
        return this;
    }
}
