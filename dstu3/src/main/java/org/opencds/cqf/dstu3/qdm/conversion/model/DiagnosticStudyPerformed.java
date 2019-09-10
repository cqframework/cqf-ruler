package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Type;

import java.util.List;

@ResourceDef(name="DiagnosticStudyPerformed", profile="TODO")
public abstract class DiagnosticStudyPerformed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public DiagnosticStudyPerformed setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="relevantPeriod", order=1)
    Period relevantPeriod;
    public Period getRelevantPeriod() {
        return relevantPeriod;
    }
    public DiagnosticStudyPerformed setRelevantPeriod(Period relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	

    @Child(name="reason", order=2)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public DiagnosticStudyPerformed setReason(Coding reason) {
        this.reason = reason;
        return this;
    }

    @Child(name="result", order=3)
    Type result;
    public Type getResult() {
        return result;
    }
    public DiagnosticStudyPerformed setResult(Type result) {
        this.result = result;
        return this;
    }

    @Child(name="resultDatetime", order=4)
    DateTimeType resultDatetime;
    public DateTimeType getResultDatetime() {
        return resultDatetime;
    }
    public DiagnosticStudyPerformed setResultDatetime(DateTimeType resultDatetime) {
        this.resultDatetime = resultDatetime;
        return this;
    }

    @Child(name="status", order=5)
    Coding status;
    public Coding getStatus() {
        return status;
    }
    public DiagnosticStudyPerformed setStatus(Coding status) {
        this.status = status;
        return this;
    }		

    @Child(name="method", order=6)
    Coding method;
    public Coding getMethod() {
        return method;
    }
    public DiagnosticStudyPerformed setMethod(Coding method) {
        this.method = method;
        return this;
    }	

    @Child(name="facilityLocation", order=7)
    Coding facilityLocation;
    public Coding getFacilityLocation() {
        return facilityLocation;
    }
    public DiagnosticStudyPerformed setFacilityLocation(Coding facilityLocation) {
        this.facilityLocation = facilityLocation;
        return this;
    }	

    @Child(name="negationRationale", order=8)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public DiagnosticStudyPerformed setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
	
    @Child(name="components", max=Child.MAX_UNLIMITED, order=9)
    List<Component> components;
    public List<Component> getComponents() {
        return components;
    }
    public DiagnosticStudyPerformed setComponents(List<Component> components) {
        this.components = components;
        return this;
    }
}
