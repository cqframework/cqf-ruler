package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Type;

import java.util.List;

@ResourceDef(name="CareGoal", profile="TODO")
public class CareGoal extends QdmBaseType {

    @Child(name="relevantPeriod", order=0)
    private Period relevantPeriod;
    public Period getRelevantPeriod() {
        return relevantPeriod;
    }
    public CareGoal setRelevantPeriod(Period relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	

    @Child(name="relatedTo", order=1)
    private List<Id> relatedTo;
    public List<Id> getRelatedTo() {
        return relatedTo;
    }
    public CareGoal setRelatedTo(List<Id> relatedTo) {
        this.relatedTo = relatedTo;
        return this;
    }
	
    @Child(name="targetOutcome", order=2)
    private Type targetOutcome;
    public Type getTargetOutcome() {
        return targetOutcome;
    }
    public CareGoal setTargetOutcome(Type targetOutcome) {
        this.targetOutcome = targetOutcome;
        return this;
    }	

    @Override
    public CareGoal copy() {
        CareGoal retVal = new CareGoal();
        super.copyValues(retVal);
        retVal.relevantPeriod = relevantPeriod;
        retVal.relatedTo = relatedTo;
        retVal.targetOutcome = targetOutcome;
        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "CareGoal";
    }
}