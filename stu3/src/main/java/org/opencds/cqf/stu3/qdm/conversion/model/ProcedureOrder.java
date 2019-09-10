package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;

@ResourceDef(name="ProcedureOrder", profile="TODO")
public abstract class ProcedureOrder extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public ProcedureOrder setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }
	
    @Child(name="reason", order=1)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public ProcedureOrder setReason(Coding reason) {
        this.reason = reason;
        return this;
    }
	
    @Child(name="method", order=2)
    Coding method;
    public Coding getMethod() {
        return method;
    }
    public ProcedureOrder setMethod(Coding method) {
        this.method = method;
        return this;
    }
	
    @Child(name="anatomicalApproachSite", order=3)
    Coding anatomicalApproachSite;
    public Coding getAnatomicalApproachSite() {
        return anatomicalApproachSite;
    }
    public ProcedureOrder setAnatomicalApproachSite(Coding anatomicalApproachSite) {
        this.anatomicalApproachSite = anatomicalApproachSite;
        return this;
    }
	
    @Child(name="anatomicalLocationSite", order=4)
    Coding anatomicalLocationSite;
    public Coding getAnatomicalLocationSite() {
        return anatomicalLocationSite;
    }
    public ProcedureOrder setAnatomicalLocationSite(Coding anatomicalLocationSite) {
        this.anatomicalLocationSite = anatomicalLocationSite;
        return this;
    }

    @Child(name="ordinality", order=5)
    Coding ordinality;
    public Coding getOrdinality() {
        return ordinality;
    }
    public ProcedureOrder setOrdinality(Coding ordinality) {
        this.ordinality = ordinality;
        return this;
    }
	
    @Child(name="negationRationale", order=6)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public ProcedureOrder setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
