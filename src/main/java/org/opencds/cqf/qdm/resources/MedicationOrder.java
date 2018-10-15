package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.qdm.QdmBaseType;
import java.lang.Integer;


@ResourceDef(name="MedicationOrder", profile="TODO")
public abstract class MedicationOrder extends QdmBaseType {

    @Child(name="activeDatetime", order=0)
    DateTime activeDatetime;
    public DateTime getActiveDatetime() {
        return activeDatetime;
    }
    public MedicationOrder setActiveDatetime(DateTime activeDatetime) {
        this.activeDatetime = activeDatetime;
        return this;
    }

	
    @Child(name="relevantPeriod", order=1)
    Interval relevantPeriod;
    public Interval getRelevantPeriod() {
        return relevantPeriod;
    }
    public MedicationOrder setRelevantPeriod(Interval relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	
	

    @Child(name="authorDatetime", order=2)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public MedicationOrder setAuthorDatetime(DateTime authorDatetime) {
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
    Code frequency;
    public Code getFrequency() {
        return frequency;
    }
    public MedicationOrder setFrequency(Code frequency) {
        this.frequency = frequency;
        return this;
    }


    @Child(name="route", order=7)
    Code route;
    public Code getRoute() {
        return route;
    }
    public MedicationOrder setRoute(Code route) {
        this.route = route;
        return this;
    }
	

    @Child(name="method", order=8)
    Code method;
    public Code getMethod() {
        return method;
    }
    public MedicationOrder setMethod(Code method) {
        this.method = method;
        return this;
    }	
	
	
    @Child(name="reason", order=9)
    Code reason;
    public Code getReason() {
        return reason;
    }
    public MedicationOrder setReason(Code reason) {
        this.reason = reason;
        return this;
    }	


	
    @Child(name="negationRationale", order=10)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public MedicationOrder setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

	
}
