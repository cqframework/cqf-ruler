package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.qdm.QdmBaseType;
import java.lang.Integer;


@ResourceDef(name="SubstanceOrder", profile="TODO")
public abstract class SubstanceOrder extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public SubstanceOrder setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }


    @Child(name="reason", order=1)
    Code reason;
    public Code getReason() {
        return reason;
    }
    public SubstanceOrder setReason(Code reason) {
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
    Code frequency;
    public Code getFrequency() {
        return frequency;
    }
    public SubstanceOrder setFrequency(Code frequency) {
        this.frequency = frequency;
        return this;
    }

	
    @Child(name="method", order=5)
    Code method;
    public Code getMethod() {
        return method;
    }
    public SubstanceOrder setMethod(Code method) {
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
    Code route;
    public Code getRoute() {
        return route;
    }
    public SubstanceOrder setRoute(Code route) {
        this.route = route;
        return this;
    }
	

    @Child(name="negationRationale", order=8)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public SubstanceOrder setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

	
}
