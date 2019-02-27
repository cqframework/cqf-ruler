package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Type;

import java.util.List;

@ResourceDef(name="AssessmentPerformed", profile="TODO")
public abstract class AssessmentPerformed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public AssessmentPerformed setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Child(name="negationRationale", order=1)
    Coding negationRationale;
    public Coding getNegationRationale() {
        return negationRationale;
    }
    public AssessmentPerformed setNegationRationale(Coding negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

	@Child(name="reason", order=2)
    Coding reason;
    public Coding getReason() {
        return reason;
    }
    public AssessmentPerformed setReason(Coding reason) {
        this.reason = reason;
        return this;
    }
	
	@Child(name="method", order=3)
    Coding method;
    public Coding getMethod() {
        return method;
    }
    public AssessmentPerformed setMethod(Coding method) {
        this.method = method;
        return this;
    }

    @Child(name="result", order=4)
    Type result;
    public Type getResult() {
        return result;
    }
    public AssessmentPerformed setResult(Type result) {
        this.result = result;
        return this;
    }
	
    @Child(name="components", max=Child.MAX_UNLIMITED, order=5)
    List<Component> components;
    public List<Component> getComponents() {
        return components;
    }
    public AssessmentPerformed setComponents(List<Component> components) {
        this.components = components;
        return this;
    }	

    @Child(name="relatedTo", max=Child.MAX_UNLIMITED, order=6)
    List<Id> relatedTo;
    public List<Id> getRelatedTo() {
        return relatedTo;
    }
    public AssessmentPerformed setRelatedTo(List<Id> relatedTo) {
        this.relatedTo = relatedTo;
        return this;
    }
}
