package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Quantity;
import java.lang.Integer;

@ResourceDef(name="MedicationDispensed", profile="TODO")
public abstract class MedicationDispensed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public MedicationDispensed setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="relevantPeriod", order=1)
    Period relevantPeriod;
    public Period getRelevantPeriod() {
        return relevantPeriod;
    }
    public MedicationDispensed setRelevantPeriod(Period relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	

    @Child(name="refills", order=2)
    Integer refills;
    public Integer getRefills() {
        return refills;
    }
    public MedicationDispensed setRefills(Integer refills) {
        this.refills = refills;
        return this;
    }

    @Child(name="dosage", order=3)
    Quantity dosage;
    public Quantity getDosage() {
        return dosage;
    }
    public MedicationDispensed setDosage(Quantity dosage) {
        this.dosage = dosage;
        return this;
    }

    @Child(name="supply", order=4)
    Quantity supply;
    public Quantity getSupply() {
        return supply;
    }
    public MedicationDispensed setSupply(Quantity supply) {
        this.supply = supply;
        return this;
    }
	
    @Child(name="frequency", order=5)
    Coding frequency;
    public Coding getFrequency() {
        return frequency;
    }
    public MedicationDispensed setFrequency(Coding frequency) {
        this.frequency = frequency;
        return this;
    }

    @Child(name="route", order=6)
    Coding route;
    public Coding getRoute() {
        return route;
    }
    public MedicationDispensed setRoute(Coding route) {
        this.route = route;
        return this;
    }

    @Child(name="negationRationale", order=7)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public MedicationDispensed setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
