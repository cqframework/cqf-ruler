package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.qdm.QdmBaseType;


@ResourceDef(name="ImmunizationAdministered", profile="TODO")
public abstract class ImmunizationAdministered extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public ImmunizationAdministered setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }


    @Child(name="reason", order=1)
    Code reason;
    public Code getReason() {
        return reason;
    }
    public ImmunizationAdministered setReason(Code reason) {
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
    Code route;
    public Code getRoute() {
        return route;
    }
    public ImmunizationAdministered setRoute(Code route) {
        this.route = route;
        return this;
    }	

	
    @Child(name="negationRationale", order=5)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public ImmunizationAdministered setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

	




	
}
