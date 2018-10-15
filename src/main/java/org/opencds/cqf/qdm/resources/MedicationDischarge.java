package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.qdm.QdmBaseType;
import java.lang.Integer;


@ResourceDef(name="MedicationDischarge", profile="TODO")
public abstract class MedicationDischarge extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public MedicationDischarge setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }


    @Child(name="refills", order=1)
    Integer refills;
    public Integer getRefills() {
        return refills;
    }
    public MedicationDischarge setRefills(Integer refills) {
        this.refills = refills;
        return this;
    }


    @Child(name="dosage", order=2)
    Quantity dosage;
    public Quantity getDosage() {
        return dosage;
    }
    public MedicationDischarge setDosage(Quantity dosage) {
        this.dosage = dosage;
        return this;
    }


    @Child(name="supply", order=3)
    Quantity supply;
    public Quantity getSupply() {
        return supply;
    }
    public MedicationDischarge setSupply(Quantity supply) {
        this.supply = supply;
        return this;
    }	
	
	
    @Child(name="frequency", order=4)
    Code frequency;
    public Code getFrequency() {
        return frequency;
    }
    public MedicationDischarge setFrequency(Code frequency) {
        this.frequency = frequency;
        return this;
    }


    @Child(name="route", order=5)
    Code route;
    public Code getRoute() {
        return route;
    }
    public MedicationDischarge setRoute(Code route) {
        this.route = route;
        return this;
    }
	

    @Child(name="negationRationale", order=6)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public MedicationDischarge setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

	
}
