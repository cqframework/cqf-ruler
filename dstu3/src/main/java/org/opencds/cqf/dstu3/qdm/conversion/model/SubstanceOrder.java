package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Quantity;
import java.lang.Integer;

@ResourceDef(name="SubstanceOrder", profile="TODO")
public abstract class SubstanceOrder extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public SubstanceOrder setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="reason", order=1)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public SubstanceOrder setReason(Coding reason) {
        this.reason = reason;
        return this;
    }

    @Child(name="dosage", order=2)
    Quantity dosage;
    public Quantity getDosage() {
        return dosage;
    }
    public SubstanceOrder setDosage(Quantity dosage) {
        this.dosage = dosage;
        return this;
    }

    @Child(name="supply", order=3)
    Quantity supply;
    public Quantity getSupply() {
        return supply;
    }
    public SubstanceOrder setSupply(Quantity supply) {
        this.supply = supply;
        return this;
    }
	
    @Child(name="frequency", order=4)
    Coding frequency;
    public Coding getFrequency() {
        return frequency;
    }
    public SubstanceOrder setFrequency(Coding frequency) {
        this.frequency = frequency;
        return this;
    }
	
    @Child(name="method", order=5)
    Coding method;
    public Coding getMethod() {
        return method;
    }
    public SubstanceOrder setMethod(Coding method) {
        this.method = method;
        return this;
    }

    @Child(name="refills", order=6)
    Integer refills;
    public Integer getRefills() {
        return refills;
    }
    public SubstanceOrder setRefills(Integer refills) {
        this.refills = refills;
        return this;
    }	

    @Child(name="route", order=7)
    Coding route;
    public Coding getRoute() {
        return route;
    }
    public SubstanceOrder setRoute(Coding route) {
        this.route = route;
        return this;
    }

    @Child(name="negationRationale", order=8)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public SubstanceOrder setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
