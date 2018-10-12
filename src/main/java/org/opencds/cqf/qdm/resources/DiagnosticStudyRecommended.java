package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.qdm.QdmBaseType;


@ResourceDef(name="DiagnosticStudyRecommended", profile="TODO")
public abstract class DiagnosticStudyRecommended extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public DiagnosticStudyRecommended setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

	
    @Child(name="method", order=1)
    Code method;
    public Code getMethod() {
        return method;
    }
    public DiagnosticStudyRecommended setMethod(Code method) {
        this.method = method;
        return this;
    }

	
    @Child(name="negationRationale", order=2)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public DiagnosticStudyRecommended setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

	




	
}
