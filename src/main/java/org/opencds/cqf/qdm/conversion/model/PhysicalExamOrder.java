package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;

@ResourceDef(name="PhysicalExamOrder", profile="TODO")
public abstract class PhysicalExamOrder extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public PhysicalExamOrder setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

	@Child(name="reason", order=1)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public PhysicalExamOrder setReason(Coding reason) {
        this.reason = reason;
        return this;
    }
	
    @Child(name="method", order=2)
    Coding method;
    public Coding getMethod() {
        return method;
    }
    public PhysicalExamOrder setMethod(Coding method) {
        this.method = method;
        return this;
    }
	
    @Child(name="anatomicalLocationSite", order=3)
    Coding anatomicalLocationSite;
    public Coding getanAtomicalLocationSite() {
        return anatomicalLocationSite;
    }
    public PhysicalExamOrder setAnatomicalLocationSite(Coding anatomicalLocationSite) {
        this.anatomicalLocationSite = anatomicalLocationSite;
        return this;
    }
	
    @Child(name="negationRationale", order=4)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public PhysicalExamOrder setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
