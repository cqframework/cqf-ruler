package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;

@ResourceDef(name="DiagnosticStudyOrder", profile="TODO")
public abstract class DiagnosticStudyOrder extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public DiagnosticStudyOrder setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="reason", order=1)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public DiagnosticStudyOrder setReason(Coding reason) {
        this.reason = reason;
        return this;
    }
	
    @Child(name="method", order=2)
    Coding method;
    public Coding getMethod() {
        return method;
    }
    public DiagnosticStudyOrder setMethod(Coding method) {
        this.method = method;
        return this;
    }
	
    @Child(name="negationRationale", order=3)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public DiagnosticStudyOrder setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
}
