package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;

@ResourceDef(name="PhysicalExamRecommended", profile="TODO")
public abstract class PhysicalExamRecommended extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public PhysicalExamRecommended setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

	@Child(name="reason", order=1)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public PhysicalExamRecommended setReason(Coding reason) {
        this.reason = reason;
        return this;
    }	

    @Child(name="method", order=2)
    Coding method;
    public Coding getMethod() {
        return method;
    }
    public PhysicalExamRecommended setMethod(Coding method) {
        this.method = method;
        return this;
    }
	
    @Child(name="anatomicalLocationSite", order=3)
    Coding anatomicalLocationSite;
    public Coding getanAtomicalLocationSite() {
        return anatomicalLocationSite;
    }
    public PhysicalExamRecommended setAnatomicalLocationSite(Coding anatomicalLocationSite) {
        this.anatomicalLocationSite = anatomicalLocationSite;
        return this;
    }
	
    @Child(name="negationRationale", order=4)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public PhysicalExamRecommended setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
