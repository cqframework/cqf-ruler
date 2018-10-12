package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.qdm.QdmBaseType;


@ResourceDef(name="AssessmentRecommended", profile="TODO")
public abstract class AssessmentRecommended extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public AssessmentRecommended setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }
	

    @Child(name="negationRationale", order=1)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public AssessmentRecommended setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }


	@Child(name="reason", order=2)
    Code reason;
    public Code getReason() {
        return reason;
    }
    public AssessmentRecommended setReason(Code reason) {
        this.reason = reason;
        return this;
    }
	
	@Child(name="method", order=3)
    Code method;
    public Code getMethod() {
        return method;
    }
    public AssessmentRecommended setMethod(Code method) {
        this.method = method;
        return this;
    }

}
