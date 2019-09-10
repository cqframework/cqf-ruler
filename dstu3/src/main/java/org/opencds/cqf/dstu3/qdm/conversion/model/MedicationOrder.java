package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Quantity;
import java.lang.Integer;

@ResourceDef(name="MedicationOrder", profile="TODO")
public abstract class MedicationOrder extends QdmBaseType {

    @Child(name="activeDatetime", order=0)
    DateTimeType activeDatetime;
    public DateTimeType getActiveDatetime() {
        return activeDatetime;
    }
    public MedicationOrder setActiveDatetime(DateTimeType activeDatetime) {
        this.activeDatetime = activeDatetime;
        return this;
    }
	
    @Child(name="relevantPeriod", order=1)
    Period relevantPeriod;
    public Period getRelevantPeriod() {
        return relevantPeriod;
    }
    public MedicationOrder setRelevantPeriod(Period relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }

    @Child(name="authorDatetime", order=2)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public MedicationOrder setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="refills", order=3)
    Integer refills;
    public Integer getRefills() {
        return refills;
    }
    public MedicationOrder setRefills(Integer refills) {
        this.refills = refills;
        return this;
    }

    @Child(name="dosage", order=4)
    Quantity dosage;
    public Quantity getDosage() {
        return dosage;
    }
    public MedicationOrder setDosage(Quantity dosage) {
        this.dosage = dosage;
        return this;
    }

    @Child(name="supply", order=5)
    Quantity supply;
    public Quantity getSupply() {
        return supply;
    }
    public MedicationOrder setSupply(Quantity supply) {
        this.supply = supply;
        return this;
    }
	
    @Child(name="frequency", order=6)
    Coding frequency;
    public Coding getFrequency() {
        return frequency;
    }
    public MedicationOrder setFrequency(Coding frequency) {
        this.frequency = frequency;
        return this;
    }

    @Child(name="route", order=7)
    Coding route;
    public Coding getRoute() {
        return route;
    }
    public MedicationOrder setRoute(Coding route) {
        this.route = route;
        return this;
    }

    @Child(name="method", order=8)
    Coding method;
    public Coding getMethod() {
        return method;
    }
    public MedicationOrder setMethod(Coding method) {
        this.method = method;
        return this;
    }	

    @Child(name="reason", order=9)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public MedicationOrder setReason(Coding reason) {
        this.reason = reason;
        return this;
    }
	
    @Child(name="negationRationale", order=10)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public MedicationOrder setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
