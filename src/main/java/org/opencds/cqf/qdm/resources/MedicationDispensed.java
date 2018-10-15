package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.qdm.QdmBaseType;
import java.lang.Integer;


@ResourceDef(name="MedicationDispensed", profile="TODO")
public abstract class MedicationDispensed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public MedicationDispensed setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }


    @Child(name="relevantPeriod", order=1)
    Interval relevantPeriod;
    public Interval getRelevantPeriod() {
        return relevantPeriod;
    }
    public MedicationDispensed setRelevantPeriod(Interval relevantPeriod) {
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
    Code frequency;
    public Code getFrequency() {
        return frequency;
    }
    public MedicationDispensed setFrequency(Code frequency) {
        this.frequency = frequency;
        return this;
    }


    @Child(name="route", order=6)
    Code route;
    public Code getRoute() {
        return route;
    }
    public MedicationDispensed setRoute(Code route) {
        this.route = route;
        return this;
    }
	

    @Child(name="negationRationale", order=7)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public MedicationDispensed setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

	
}
