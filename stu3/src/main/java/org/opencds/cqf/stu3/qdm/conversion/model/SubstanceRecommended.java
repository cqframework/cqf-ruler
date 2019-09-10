package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Quantity;
import java.lang.Integer;

@ResourceDef(name="SubstanceRecommended", profile="TODO")
public abstract class SubstanceRecommended extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public SubstanceRecommended setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="reason", order=1)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public SubstanceRecommended setReason(Coding reason) {
        this.reason = reason;
        return this;
    }

    @Child(name="dosage", order=2)
    Quantity dosage;
    public Quantity getDosage() {
        return dosage;
    }
    public SubstanceRecommended setDosage(Quantity dosage) {
        this.dosage = dosage;
        return this;
    }

    @Child(name="supply", order=3)
    Quantity supply;
    public Quantity getSupply() {
        return supply;
    }
    public SubstanceRecommended setSupply(Quantity supply) {
        this.supply = supply;
        return this;
    }
	
    @Child(name="frequency", order=4)
    Coding frequency;
    public Coding getFrequency() {
        return frequency;
    }
    public SubstanceRecommended setFrequency(Coding frequency) {
        this.frequency = frequency;
        return this;
    }

    @Child(name="method", order=5)
    Coding method;
    public Coding getMethod() {
        return method;
    }
    public SubstanceRecommended setMethod(Coding method) {
        this.method = method;
        return this;
    }

    @Child(name="refills", order=6)
    Integer refills;
    public Integer getRefills() {
        return refills;
    }
    public SubstanceRecommended setRefills(Integer refills) {
        this.refills = refills;
        return this;
    }
	
    @Child(name="route", order=7)
    Coding route;
    public Coding getRoute() {
        return route;
    }
    public SubstanceRecommended setRoute(Coding route) {
        this.route = route;
        return this;
    }

    @Child(name="negationRationale", order=8)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public SubstanceRecommended setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
