package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;

@ResourceDef(name="ProcedureRecommended", profile="TODO")
public abstract class ProcedureRecommended extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public ProcedureRecommended setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="reason", order=1)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public ProcedureRecommended setReason(Coding reason) {
        this.reason = reason;
        return this;
    }
	
    @Child(name="method", order=2)
    Coding method;
    public Coding getMethod() {
        return method;
    }
    public ProcedureRecommended setMethod(Coding method) {
        this.method = method;
        return this;
    }
	
    @Child(name="anatomicalApproachSite", order=3)
    Coding anatomicalApproachSite;
    public Coding getAnatomicalApproachSite() {
        return anatomicalApproachSite;
    }
    public ProcedureRecommended setAnatomicalApproachSite(Coding anatomicalApproachSite) {
        this.anatomicalApproachSite = anatomicalApproachSite;
        return this;
    }
	
    @Child(name="anatomicalLocationSite", order=4)
    Coding anatomicalLocationSite;
    public Coding getAnatomicalLocationSite() {
        return anatomicalLocationSite;
    }
    public ProcedureRecommended setAnatomicalLocationSite(Coding anatomicalLocationSite) {
        this.anatomicalLocationSite = anatomicalLocationSite;
        return this;
    }

    @Child(name="ordinality", order=5)
    Coding ordinality;
    public Coding getOrdinality() {
        return ordinality;
    }
    public ProcedureRecommended setOrdinality(Coding ordinality) {
        this.ordinality = ordinality;
        return this;
    }
	
    @Child(name="negationRationale", order=6)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public ProcedureRecommended setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
