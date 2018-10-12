package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.qdm.QdmBaseType;
import org.opencds.cqf.qdm.types.Component;
import org.opencds.cqf.qdm.types.Id;

import java.util.List;

@ResourceDef(name="AssessmentPerformed", profile="TODO")
public abstract class AssessmentPerformed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public AssessmentPerformed setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }
	

    @Child(name="negationRationale", order=1)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public AssessmentPerformed setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }


	@Child(name="reason", order=2)
    Code reason;
    public Code getReason() {
        return reason;
    }
    public AssessmentPerformed setReason(Code reason) {
        this.reason = reason;
        return this;
    }
	
	@Child(name="method", order=3)
    Code method;
    public Code getMethod() {
        return method;
    }
    public AssessmentPerformed setMethod(Code method) {
        this.method = method;
        return this;
    }

	
    @Child(name="result", order=4)
    Object result;
    public Object getResult() {
        return result;
    }
    public AssessmentPerformed setResult(Object result) {
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
