package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.qdm.QdmBaseType;


@ResourceDef(name="ImmunizationOrder", profile="TODO")
public abstract class ImmunizationOrder extends QdmBaseType {

    @Child(name="activeDatetime", order=0)
    DateTime activeDatetime;
    public DateTime getActiveDatetime() {
        return activeDatetime;
    }
    public ImmunizationOrder setActiveDatetime(DateTime activeDatetime) {
        this.activeDatetime = activeDatetime;
        return this;
    }


    @Child(name="authorDatetime", order=1)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public ImmunizationOrder setAuthorDatetime(DateTime authorDatetime) {
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
    Code reason;
    public Code getReason() {
        return reason;
    }
    public ImmunizationOrder setReason(Code reason) {
        this.reason = reason;
        return this;
    }


    @Child(name="route", order=5)
    Code route;
    public Code getRoute() {
        return route;
    }
    public ImmunizationOrder setRoute(Code route) {
        this.route = route;
        return this;
    }	

	
    @Child(name="negationRationale", order=6)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public ImmunizationOrder setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

	




	
}
