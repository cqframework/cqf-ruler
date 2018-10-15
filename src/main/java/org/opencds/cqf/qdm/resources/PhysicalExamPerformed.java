package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.qdm.QdmBaseType;
import org.opencds.cqf.qdm.types.Component;

import java.util.List;


@ResourceDef(name="PhysicalExamPerformed", profile="TODO")
public abstract class PhysicalExamPerformed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public PhysicalExamPerformed setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }
	

    @Child(name="relevantPeriod", order=1)
    Interval relevantPeriod;
    public Interval getRelevantPeriod() {
        return relevantPeriod;
    }
    public PhysicalExamPerformed setRelevantPeriod(Interval relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	
	
	
	@Child(name="reason", order=2)
    Code reason;
    public Code getReason() {
        return reason;
    }
    public PhysicalExamPerformed setReason(Code reason) {
        this.reason = reason;
        return this;
    }	
	
	
    @Child(name="method", order=3)
    Code method;
    public Code getMethod() {
        return method;
    }
    public PhysicalExamPerformed setMethod(Code method) {
        this.method = method;
        return this;
    }	
	

    @Child(name="result", order=4)
    Object result;
    public Object getResult() {
        return result;
    }
    public PhysicalExamPerformed setResult(Object result) {
        this.result = result;
        return this;
    }

	
    @Child(name="anatomicalLocationSite", order=5)
    Code anatomicalLocationSite;
    public Code getanAtomicalLocationSite() {
        return anatomicalLocationSite;
    }
    public PhysicalExamPerformed setAnatomicalLocationSite(Code anatomicalLocationSite) {
        this.anatomicalLocationSite = anatomicalLocationSite;
        return this;
    }	

	
    @Child(name="negationRationale", order=6)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public PhysicalExamPerformed setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }


    @Child(name="components", max=Child.MAX_UNLIMITED, order=7)
    List<Component> components;
    public List<Component> getComponents() {
        return components;
    }
    public PhysicalExamPerformed setComponents(List<Component> components) {
        this.components = components;
        return this;
    }
	
	
}
