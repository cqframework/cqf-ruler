package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.qdm.QdmBaseType;


@ResourceDef(name="LaboratoryTestOrder", profile="TODO")
public abstract class LaboratoryTestOrder extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public LaboratoryTestOrder setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }


    @Child(name="reason", order=1)
    Code reason;
    public Code getReason() {
        return reason;
    }
    public LaboratoryTestOrder setReason(Code reason) {
        this.reason = reason;
        return this;
    }
	

    @Child(name="method", order=2)
    Code method;
    public Code getMethod() {
        return method;
    }
    public LaboratoryTestOrder setMethod(Code method) {
        this.method = method;
        return this;
    }


    @Child(name="negationRationale", order=3)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public LaboratoryTestOrder setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

	
}
