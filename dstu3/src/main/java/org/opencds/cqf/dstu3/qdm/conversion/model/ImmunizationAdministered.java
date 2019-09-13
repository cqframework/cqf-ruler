package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Quantity;


@ResourceDef(name="ImmunizationAdministered", profile="TODO")
public abstract class ImmunizationAdministered extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public ImmunizationAdministered setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="reason", order=1)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public ImmunizationAdministered setReason(Coding reason) {
        this.reason = reason;
        return this;
    }	

    @Child(name="dosage", order=2)
    Quantity dosage;
    public Quantity getDosage() {
        return dosage;
    }
    public ImmunizationAdministered setDosage(Quantity dosage) {
        this.dosage = dosage;
        return this;
    }

    @Child(name="supply", order=3)
    Quantity supply;
    public Quantity getSupply() {
        return supply;
    }
    public ImmunizationAdministered setSupply(Quantity supply) {
        this.supply = supply;
        return this;
    }

    @Child(name="route", order=4)
    Coding route;
    public Coding getRoute() {
        return route;
    }
    public ImmunizationAdministered setRoute(Coding route) {
        this.route = route;
        return this;
    }
	
    @Child(name="negationRationale", order=5)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public ImmunizationAdministered setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
