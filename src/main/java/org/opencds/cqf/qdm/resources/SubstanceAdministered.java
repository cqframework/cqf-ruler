package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.qdm.QdmBaseType;


@ResourceDef(name="SubstanceAdministered", profile="TODO")
public abstract class SubstanceAdministered extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public SubstanceAdministered setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }


    @Child(name="relevantPeriod", order=1)
    Interval relevantPeriod;
    public Interval getRelevantPeriod() {
        return relevantPeriod;
    }
    public SubstanceAdministered setRelevantPeriod(Interval relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	
	
	
    @Child(name="dosage", order=2)
    Quantity dosage;
    public Quantity getDosage() {
        return dosage;
    }
    public SubstanceAdministered setDosage(Quantity dosage) {
        this.dosage = dosage;
        return this;
    }


    @Child(name="supply", order=3)
    Quantity supply;
    public Quantity getSupply() {
        return supply;
    }
    public SubstanceAdministered setSupply(Quantity supply) {
        this.supply = supply;
        return this;
    }	
	
	
    @Child(name="frequency", order=4)
    Code frequency;
    public Code getFrequency() {
        return frequency;
    }
    public SubstanceAdministered setFrequency(Code frequency) {
        this.frequency = frequency;
        return this;
    }
	
	
    @Child(name="route", order=5)
    Code route;
    public Code getRoute() {
        return route;
    }
    public SubstanceAdministered setRoute(Code route) {
        this.route = route;
        return this;
    }
	

    @Child(name="negationRationale", order=6)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public SubstanceAdministered setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

	
}
