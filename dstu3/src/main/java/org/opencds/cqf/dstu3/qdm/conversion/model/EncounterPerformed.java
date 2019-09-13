package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Quantity;

import java.util.List;

public abstract class EncounterPerformed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public EncounterPerformed setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="admissionSource", min=1, order=1)
    Coding admissionSource;
    public Coding getAdmissionSource() {
        return admissionSource;
    }
    public EncounterPerformed setAdmissionSource(Coding admissionSource) {
        this.admissionSource = admissionSource;
        return this;
    }

    @Child(name="relevantPeriod", order=2)
    Period relevantPeriod;
    public Period getRelevantPeriod() {
        return relevantPeriod;
    }
    public EncounterPerformed setRelevantPeriod(Period relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }

    @Child(name="dischargeDisposition", order=3)
    Coding dischargeDisposition;
    public Coding getDischargeDisposition() {
        return dischargeDisposition;
    }
    public EncounterPerformed setDischargeDisposition(Coding dischargeDisposition) {
        this.dischargeDisposition = dischargeDisposition;
        return this;
    }

    @Child(name="diagnoses", max=Child.MAX_UNLIMITED, order=4)
    List<Coding> diagnoses;
    public List<Coding> getDiagnoses() {
        return diagnoses;
    }
    public EncounterPerformed setDiagnoses(List<Coding> diagnoses) {
        this.diagnoses = diagnoses;
        return this;
    }

    @Child(name="facilityLocations", max=Child.MAX_UNLIMITED, order=5)
    List<FacilityLocation> facilityLocation;
    public List<FacilityLocation> getFacilityLocations() {
        return facilityLocation;
    }
    public EncounterPerformed setFacilityLocations(List<FacilityLocation> facilityLocation) {
        this.facilityLocation = facilityLocation;
        return this;
    }

    @Child(name="principalDiagnosis", order=6)
    Coding principalDiagnosis;
    public Coding getPrincipalDiagnosis() {
        return principalDiagnosis;
    }
    public EncounterPerformed setPrincipalDiagnosis(Coding principalDiagnosis) {
        this.principalDiagnosis = principalDiagnosis;
        return this;
    }

    @Child(name="negationRationale", order=7)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public EncounterPerformed setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

    @Child(name="lengthOfStay", order=8)
    Quantity lengthOfStay;
    public Quantity getLengthOfStay() {
        return lengthOfStay;
    }
    public EncounterPerformed setLengthOfStay(Quantity lengthOfStay) {
        this.lengthOfStay = lengthOfStay;
        return this;
    }
}
