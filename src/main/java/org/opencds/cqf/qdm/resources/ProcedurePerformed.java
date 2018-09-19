package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.qdm.QdmBaseType;
import org.opencds.cqf.qdm.types.Component;

import java.util.List;

@ResourceDef(name="ProcedurePerformed", profile="TODO")
public abstract class ProcedurePerformed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public ProcedurePerformed setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

	
    @Child(name="relevantPeriod", order=1)
    Interval relevantPeriod;
    public Interval getRelevantPeriod() {
        return relevantPeriod;
    }
    public ProcedurePerformed setRelevantPeriod(Interval relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	
	

    @Child(name="reason", order=2)
    Code reason;
    public Code getReason() {
        return reason;
    }
    public ProcedurePerformed setReason(Code reason) {
        this.reason = reason;
        return this;
    }

	
    @Child(name="method", order=3)
    Code method;
    public Code getMethod() {
        return method;
    }
    public ProcedurePerformed setMethod(Code method) {
        this.method = method;
        return this;
    }	


    @Child(name="result", order=4)
    Object result;
    public Object getResult() {
        return result;
    }
    public ProcedurePerformed setResult(Object result) {
        this.result = result;
        return this;
    }
	

    @Child(name="status", order=5)
    Code status;
    public Code getStatus() {
        return status;
    }
    public ProcedurePerformed setStatus(Code status) {
        this.status = status;
        return this;
    }		

	
    @Child(name="anatomicalApproachSite", order=6)
    Code anatomicalApproachSite;
    public Code getAnatomicalApproachSite() {
        return anatomicalApproachSite;
    }
    public ProcedurePerformed setAnatomicalApproachSite(Code anatomicalApproachSite) {
        this.anatomicalApproachSite = anatomicalApproachSite;
        return this;
    }

	
    @Child(name="anatomicalLocationSite", order=7)
    Code anatomicalLocationSite;
    public Code getAnatomicalLocationSite() {
        return anatomicalLocationSite;
    }
    public ProcedurePerformed setAnatomicalLocationSite(Code anatomicalLocationSite) {
        this.anatomicalLocationSite = anatomicalLocationSite;
        return this;
    }


    @Child(name="ordinality", order=8)
    Code ordinality;
    public Code getOrdinality() {
        return ordinality;
    }
    public ProcedurePerformed setOrdinality(Code ordinality) {
        this.ordinality = ordinality;
        return this;
    }	

    @Child(name="incisionDatetime", order=9)
    DateTime incisionDatetime;
    public DateTime getIncisionDatetime() {
        return incisionDatetime;
    }
    public ProcedurePerformed setIncisionDatetime(DateTime incisionDatetime) {
        this.incisionDatetime = incisionDatetime;
        return this;
    }	
	
	
    @Child(name="negationRationale", order=10)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public ProcedurePerformed setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
	
	
    @Child(name="components", max=Child.MAX_UNLIMITED, order=11)
    List<Component> components;
    public List<Component> getComponents() {
        return components;
    }
    public ProcedurePerformed setComponents(List<Component> components) {
        this.components = components;
        return this;
    }
	


}
