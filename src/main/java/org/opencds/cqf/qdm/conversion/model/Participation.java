package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="Participation", profile="TODO")
public class Participation extends QdmBaseType {

    @Child(name="participationPeriod", order=0)
    private Period participationPeriod;
    public Period getParticipationPeriod() {
        return participationPeriod;
    }
    public Participation setParticipationPeriod(Period participationPeriod) {
        this.participationPeriod = participationPeriod;
        return this;
    }	

    @Override
    public Participation copy() {
        Participation retVal = new Participation();
        super.copyValues(retVal);
        retVal.participationPeriod = participationPeriod;
        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "Participation";
    }
}