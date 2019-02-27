package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Quantity;
import java.lang.Integer;

@ResourceDef(name="MedicationDischarge", profile="TODO")
public abstract class MedicationDischarge extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public MedicationDischarge setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="refills", order=1)
    Integer refills;
    public Integer getRefills() {
        return refills;
    }
    public MedicationDischarge setRefills(Integer refills) {
        this.refills = refills;
        return this;
    }

    @Child(name="dosage", order=2)
    Quantity dosage;
    public Quantity getDosage() {
        return dosage;
    }
    public MedicationDischarge setDosage(Quantity dosage) {
        this.dosage = dosage;
        return this;
    }

    @Child(name="supply", order=3)
    Quantity supply;
    public Quantity getSupply() {
        return supply;
    }
    public MedicationDischarge setSupply(Quantity supply) {
        this.supply = supply;
        return this;
    }
	
    @Child(name="frequency", order=4)
    Coding frequency;
    public Coding getFrequency() {
        return frequency;
    }
    public MedicationDischarge setFrequency(Coding frequency) {
        this.frequency = frequency;
        return this;
    }

    @Child(name="route", order=5)
    Coding route;
    public Coding getRoute() {
        return route;
    }
    public MedicationDischarge setRoute(Coding route) {
        this.route = route;
        return this;
    }

    @Child(name="negationRationale", order=6)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public MedicationDischarge setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
