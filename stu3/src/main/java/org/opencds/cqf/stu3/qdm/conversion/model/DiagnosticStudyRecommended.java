package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;

@ResourceDef(name="DiagnosticStudyRecommended", profile="TODO")
public abstract class DiagnosticStudyRecommended extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public DiagnosticStudyRecommended setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }
	
    @Child(name="method", order=1)
    Coding method;
    public Coding getMethod() {
        return method;
    }
    public DiagnosticStudyRecommended setMethod(Coding method) {
        this.method = method;
        return this;
    }
	
    @Child(name="negationRationale", order=2)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public DiagnosticStudyRecommended setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
