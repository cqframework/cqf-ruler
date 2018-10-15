package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.qdm.QdmBaseType;


@ResourceDef(name="PhysicalExamRecommended", profile="TODO")
public abstract class PhysicalExamRecommended extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public PhysicalExamRecommended setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }
	

	@Child(name="reason", order=1)
    Code reason;
    public Code getReason() {
        return reason;
    }
    public PhysicalExamRecommended setReason(Code reason) {
        this.reason = reason;
        return this;
    }	
	
	
    @Child(name="method", order=2)
    Code method;
    public Code getMethod() {
        return method;
    }
    public PhysicalExamRecommended setMethod(Code method) {
        this.method = method;
        return this;
    }	
	
	
    @Child(name="anatomicalLocationSite", order=3)
    Code anatomicalLocationSite;
    public Code getanAtomicalLocationSite() {
        return anatomicalLocationSite;
    }
    public PhysicalExamRecommended setAnatomicalLocationSite(Code anatomicalLocationSite) {
        this.anatomicalLocationSite = anatomicalLocationSite;
        return this;
    }	

	
    @Child(name="negationRationale", order=4)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public PhysicalExamRecommended setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

	
}
