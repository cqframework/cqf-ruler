package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Quantity;

@ResourceDef(name="ImmunizationOrder", profile="TODO")
public abstract class ImmunizationOrder extends QdmBaseType {

    @Child(name="activeDatetime", order=0)
    DateTimeType activeDatetime;
    public DateTimeType getActiveDatetime() {
        return activeDatetime;
    }
    public ImmunizationOrder setActiveDatetime(DateTimeType activeDatetime) {
        this.activeDatetime = activeDatetime;
        return this;
    }

    @Child(name="authorDatetime", order=1)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public ImmunizationOrder setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="dosage", order=2)
    Quantity dosage;
    public Quantity getDosage() {
        return dosage;
    }
    public ImmunizationOrder setDosage(Quantity dosage) {
        this.dosage = dosage;
        return this;
    }

    @Child(name="supply", order=3)
    Quantity supply;
    public Quantity getSupply() {
        return supply;
    }
    public ImmunizationOrder setSupply(Quantity supply) {
        this.supply = supply;
        return this;
    }
	
    @Child(name="reason", order=4)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public ImmunizationOrder setReason(Coding reason) {
        this.reason = reason;
        return this;
    }

    @Child(name="route", order=5)
    Coding route;
    public Coding getRoute() {
        return route;
    }
    public ImmunizationOrder setRoute(Coding route) {
        this.route = route;
        return this;
    }
	
    @Child(name="negationRationale", order=6)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public ImmunizationOrder setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
