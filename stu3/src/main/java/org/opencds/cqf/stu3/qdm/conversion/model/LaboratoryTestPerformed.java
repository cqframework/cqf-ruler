package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import org.hl7.fhir.dstu3.model.*;

import java.util.List;

public abstract class LaboratoryTestPerformed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public LaboratoryTestPerformed setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="relevantPeriod", order=1)
    Period relevantPeriod;
    public Period getRelevantPeriod() {
        return relevantPeriod;
    }
    public LaboratoryTestPerformed setRelevantPeriod(Period relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }
	
    @Child(name="status", order=2)
    Coding status;
    public Coding getStatus() {
        return status;
    }
    public LaboratoryTestPerformed setStatus(Coding status) {
        this.status = status;
        return this;
    }	

    @Child(name="method", order=3)
    Coding method;
    public Coding getMethod() {
        return method;
    }
    public LaboratoryTestPerformed setMethod(Coding method) {
        this.method = method;
        return this;
    }		

    @Child(name="result", order=4)
    Type result;
    public Type getResult() {
        return result;
    }
    public LaboratoryTestPerformed setResult(Type result) {
        this.result = result;
        return this;
    }
	
    @Child(name="resultDatetime", order=5)
    DateTimeType resultDatetime;
    public DateTimeType getResultDatetime() {
        return resultDatetime;
    }
    public LaboratoryTestPerformed setResultDatetime(DateTimeType resultDatetime) {
        this.resultDatetime = resultDatetime;
        return this;
    }
	
    @Child(name="reason", order=6)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public LaboratoryTestPerformed setReason(Coding reason) {
        this.reason = reason;
        return this;
    }
	
    @Child(name="referenceRange", order=7)
    Range referenceRange;
    public Range getReferenceRange() {
        return referenceRange;
    }
    public LaboratoryTestPerformed setReferenceRange(Range referenceRange) {
        this.referenceRange = referenceRange;
        return this;
    }
	
    @Child(name="negationRationale", order=8)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public LaboratoryTestPerformed setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
	
    @Child(name="components", max=Child.MAX_UNLIMITED, order=9)
    List<ResultComponent> components;
    public List<ResultComponent> getComponents() {
        return components;
    }
    public LaboratoryTestPerformed setComponents(List<ResultComponent> components) {
        this.components = components;
        return this;
    }
}
