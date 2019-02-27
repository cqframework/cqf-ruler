package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Coding;

@ResourceDef(name="MedicationActive", profile="TODO")
public class MedicationActive extends QdmBaseType {

    @Child(name="relevantPeriod", order=0)
    private Period relevantPeriod;
    public Period getRelevantPeriod() {
        return relevantPeriod;
    }
    public MedicationActive setRelevantPeriod(Period relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }

	@Child(name="dosage", order=1)
    private Quantity dosage;
    public Quantity getDosage() {
        return dosage;
    }
    public MedicationActive setDosage(Quantity dosage) {
        this.dosage = dosage;
        return this;
    }

	@Child(name="supply", order=2)
    private Quantity supply;
    public Quantity getSupply() {
        return supply;
    }
    public MedicationActive setSupply(Quantity supply) {
        this.supply = supply;
        return this;
    }
	
    @Child(name="frequency", order=3)
    private Coding frequency;
    public Coding getFrequency() {
        return frequency;
    }
    public MedicationActive setFrequency(Coding frequency) {
        this.frequency = frequency;
        return this;
    }

    @Child(name="route", order=4)
    private Coding route;
    public Coding getRoute() {
        return route;
    }
    public MedicationActive setRoute(Coding route) {
        this.route = route;
        return this;
    }
	
    @Override
    public MedicationActive copy() {
        MedicationActive retVal = new MedicationActive();
        super.copyValues(retVal);
        retVal.relevantPeriod = relevantPeriod;
        retVal.dosage = dosage;
        retVal.supply = supply;
        retVal.frequency = frequency;
        retVal.route = route;
        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "MedicationActive";
    }
}