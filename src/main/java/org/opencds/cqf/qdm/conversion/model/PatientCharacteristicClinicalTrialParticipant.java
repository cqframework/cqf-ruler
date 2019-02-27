package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Period;

@ResourceDef(name="PatientCharacteristicClinicalTrialParticipant", profile="TODO")
public class PatientCharacteristicClinicalTrialParticipant extends QdmBaseType {

    @Child(name="reason", order=0)
    private Coding reason;
    public Coding getReason() {
        return reason;
    }
    public PatientCharacteristicClinicalTrialParticipant setReason(Coding reason) {
        this.reason = reason;
        return this;
    }

    @Child(name="relevantPeriod", order=1)
    private Period relevantPeriod;
    public Period getRelevantPeriod() {
        return relevantPeriod;
    }
    public PatientCharacteristicClinicalTrialParticipant setRelevantPeriod(Period relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }

    @Override
    public PatientCharacteristicClinicalTrialParticipant copy() {
        PatientCharacteristicClinicalTrialParticipant retVal = new PatientCharacteristicClinicalTrialParticipant();
        super.copyValues(retVal);
        retVal.reason = reason;
        retVal.relevantPeriod = relevantPeriod;
        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PatientCharacteristicClinicalTrialParticipant";
    }
}