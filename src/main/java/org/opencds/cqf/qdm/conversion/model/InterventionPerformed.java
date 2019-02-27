package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Type;

public abstract class InterventionPerformed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public InterventionPerformed setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="relevantPeriod", order=1)
    Period relevantPeriod;
    public Period getRelevantPeriod() {
        return relevantPeriod;
    }
    public InterventionPerformed setRelevantPeriod(Period relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	

    @Child(name="reason", order=2)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public InterventionPerformed setReason(Coding reason) {
        this.reason = reason;
        return this;
    }
	
    @Child(name="result", order=3)
    Type result;
    public Type getResult() {
        return result;
    }
    public InterventionPerformed setResult(Type result) {
        this.result = result;
        return this;
    }

    @Child(name="status", order=4)
    Coding status;
    public Coding getStatus() {
        return status;
    }
    public InterventionPerformed setStatus(Coding status) {
        this.status = status;
        return this;
    }	

    @Child(name="negationRationale", order=5)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public InterventionPerformed setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
