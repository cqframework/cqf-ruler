package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.qdm.QdmBaseType;


@ResourceDef(name="InterventionRecommended", profile="TODO")
public abstract class InterventionRecommended extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public InterventionRecommended setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

	
    @Child(name="reason", order=1)
    Code reason;
    public Code getReason() {
        return reason;
    }
    public InterventionRecommended setReason(Code reason) {
        this.reason = reason;
        return this;
    }

	
    @Child(name="negationRationale", order=2)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public InterventionRecommended setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

	




	
}
