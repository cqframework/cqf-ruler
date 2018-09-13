package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.qdm.QdmBaseType;
import org.opencds.cqf.qdm.types.FacilityLocation;

import java.util.List;

@ResourceDef(name="PositiveEncounterPerformed", profile="TODO")
public class PositiveEncounterPerformed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    private DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public PositiveEncounterPerformed setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="admissionSource", min=1, order=1)
    private Code admissionSource;
    public Code getAdmissionSource() {
        return admissionSource;
    }
    public PositiveEncounterPerformed setAdmissionSource(Code admissionSource) {
        this.admissionSource = admissionSource;
        return this;
    }

    @Child(name="relevantPeriod", order=2)
    private Interval relevantPeriod;
    public Interval getRelevantPeriod() {
        return relevantPeriod;
    }
    public PositiveEncounterPerformed setRelevantPeriod(Interval relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }

    @Child(name="dischargeDisposition", order=3)
    private Code dischargeDisposition;
    public Code getDischargeDisposition() {
        return dischargeDisposition;
    }
    public PositiveEncounterPerformed setDischargeDisposition(Code dischargeDisposition) {
        this.dischargeDisposition = dischargeDisposition;
        return this;
    }

    @Child(name="diagnosis", max=Child.MAX_UNLIMITED, order=4)
    private List<Code> diagnosis;
    public List<Code> getDiagnosis() {
        return diagnosis;
    }
    public PositiveEncounterPerformed setDiagnosis(List<Code> diagnosis) {
        this.diagnosis = diagnosis;
        return this;
    }

    @Child(name="facilityLocations", max=Child.MAX_UNLIMITED, order=5)
    private List<FacilityLocation> facilityLocation;
    public List<FacilityLocation> getFacilityLocation() {
        return facilityLocation;
    }
    public PositiveEncounterPerformed setFacilityLocations(List<FacilityLocation> facilityLocation) {
        this.facilityLocation = facilityLocation;
        return this;
    }

    @Child(name="principalDiagnosis", order=6)
    private Code principalDiagnosis;
    public Code getPrincipalDiagnosis() {
        return principalDiagnosis;
    }
    public PositiveEncounterPerformed setPrincipalDiagnosis(Code principalDiagnosis) {
        this.principalDiagnosis = principalDiagnosis;
        return this;
    }

    @Child(name="negationRationale", order=7)
    private Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public PositiveEncounterPerformed setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

    @Child(name="lengthOfStay", order=8)
    private Quantity lengthOfStay;
    public Quantity getLengthOfStay() {
        return lengthOfStay;
    }
    public PositiveEncounterPerformed setLengthOfStay(Quantity lengthOfStay) {
        this.lengthOfStay = lengthOfStay;
        return this;
    }

    @Override
    public PositiveEncounterPerformed copy() {
        PositiveEncounterPerformed retVal = new PositiveEncounterPerformed();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.admissionSource = admissionSource;
        retVal.relevantPeriod = relevantPeriod;
        retVal.dischargeDisposition = dischargeDisposition;
        retVal.diagnosis = diagnosis;
        retVal.facilityLocation = facilityLocation;
        retVal.principalDiagnosis = principalDiagnosis;
        retVal.negationRationale = negationRationale;
        retVal.lengthOfStay = lengthOfStay;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }
}
