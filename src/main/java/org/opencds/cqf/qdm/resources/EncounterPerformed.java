package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.qdm.QdmBaseType;
import org.opencds.cqf.qdm.types.FacilityLocation;

import java.util.List;

public abstract class EncounterPerformed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public EncounterPerformed setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="admissionSource", min=1, order=1)
    Code admissionSource;
    public Code getAdmissionSource() {
        return admissionSource;
    }
    public EncounterPerformed setAdmissionSource(Code admissionSource) {
        this.admissionSource = admissionSource;
        return this;
    }

    @Child(name="relevantPeriod", order=2)
    Interval relevantPeriod;
    public Interval getRelevantPeriod() {
        return relevantPeriod;
    }
    public EncounterPerformed setRelevantPeriod(Interval relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }

    @Child(name="dischargeDisposition", order=3)
    Code dischargeDisposition;
    public Code getDischargeDisposition() {
        return dischargeDisposition;
    }
    public EncounterPerformed setDischargeDisposition(Code dischargeDisposition) {
        this.dischargeDisposition = dischargeDisposition;
        return this;
    }

    @Child(name="diagnosis", max=Child.MAX_UNLIMITED, order=4)
    List<Code> diagnosis;
    public List<Code> getDiagnosis() {
        return diagnosis;
    }
    public EncounterPerformed setDiagnosis(List<Code> diagnosis) {
        this.diagnosis = diagnosis;
        return this;
    }

    @Child(name="facilityLocations", max=Child.MAX_UNLIMITED, order=5)
    List<FacilityLocation> facilityLocation;
    public List<FacilityLocation> getFacilityLocation() {
        return facilityLocation;
    }
    public EncounterPerformed setFacilityLocations(List<FacilityLocation> facilityLocation) {
        this.facilityLocation = facilityLocation;
        return this;
    }

    @Child(name="principalDiagnosis", order=6)
    Code principalDiagnosis;
    public Code getPrincipalDiagnosis() {
        return principalDiagnosis;
    }
    public EncounterPerformed setPrincipalDiagnosis(Code principalDiagnosis) {
        this.principalDiagnosis = principalDiagnosis;
        return this;
    }

    @Child(name="negationRationale", order=7)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public EncounterPerformed setNegationRationale(Code negationRationale) {
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
