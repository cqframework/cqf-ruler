package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Quantity;

@ResourceDef(name="MedicationAdministered", profile="TODO")
public abstract class MedicationAdministered extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public MedicationAdministered setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="relevantPeriod", order=1)
    Period relevantPeriod;
    public Period getRelevantPeriod() {
        return relevantPeriod;
    }
    public MedicationAdministered setRelevantPeriod(Period relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	

    @Child(name="dosage", order=2)
    Quantity dosage;
    public Quantity getDosage() {
        return dosage;
    }
    public MedicationAdministered setDosage(Quantity dosage) {
        this.dosage = dosage;
        return this;
    }

    @Child(name="supply", order=3)
    Quantity supply;
    public Quantity getSupply() {
        return supply;
    }
    public MedicationAdministered setSupply(Quantity supply) {
        this.supply = supply;
        return this;
    }
	
    @Child(name="frequency", order=4)
    Coding frequency;
    public Coding getFrequency() {
        return frequency;
    }
    public MedicationAdministered setFrequency(Coding frequency) {
        this.frequency = frequency;
        return this;
    }

    @Child(name="route", order=5)
    Coding route;
    public Coding getRoute() {
        return route;
    }
    public MedicationAdministered setRoute(Coding route) {
        this.route = route;
        return this;
    }

    @Child(name="reason", order=6)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public MedicationAdministered setReason(Coding reason) {
        this.reason = reason;
        return this;
    }
	
    @Child(name="negationRationale", order=7)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public MedicationAdministered setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
