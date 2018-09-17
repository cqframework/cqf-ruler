package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.qdm.QdmBaseType;
import org.opencds.cqf.qdm.types.FacilityLocation;

import java.util.List;

@ResourceDef(name="ProcedurePerformed", profile="TODO")
public abstract class ProcedurePerformed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    private DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public ProcedurePerformed setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

	
    @Child(name="relevantPeriod", order=1)
    private Interval relevantPeriod;
    public Interval getRelevantPeriod() {
        return relevantPeriod;
    }
    public ProcedurePerformed setRelevantPeriod(Interval relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	
	

    @Child(name="reason", order=2)
    private Code reason;
    public Code getReason() {
        return reason;
    }
    public ProcedurePerformed setReason(Code reason) {
        this.reason = reason;
        return this;
    }

	
    @Child(name="method", order=3)
    private Code method;
    public Code getMethod() {
        return method;
    }
    public ProcedurePerformed setMethod(Code method) {
        this.method = method;
        return this;
    }	


    @Child(name="status", order=4)
    private Code status;
    public Code getStatus() {
        return status;
    }
    public ProcedurePerformed setStatus(Code status) {
        this.status = status;
        return this;
    }		

	
    @Child(name="anatomicalApproachSite", order=5)
    private Code anatomicalApproachSite;
    public Code getAnatomicalApproachSite() {
        return anatomicalApproachSite;
    }
    public ProcedurePerformed setAnatomicalApproachSite(Code anatomicalApproachSite) {
        this.anatomicalApproachSite = anatomicalApproachSite;
        return this;
    }

	
    @Child(name="anatomicalLocationSite", order=6)
    private Code anatomicalLocationSite;
    public Code getAnatomicalLocationSite() {
        return anatomicalLocationSite;
    }
    public ProcedurePerformed setAnatomicalLocationSite(Code anatomicalLocationSite) {
        this.anatomicalLocationSite = anatomicalLocationSite;
        return this;
    }


    @Child(name="ordinality", order=7)
    private Code ordinality;
    public Code getOrdinality() {
        return ordinality;
    }
    public ProcedurePerformed setOrdinality(Code ordinality) {
        this.ordinality = ordinality;
        return this;
    }	

    @Child(name="incisionDatetime", order=8)
    private DateTime incisionDatetime;
    public DateTime getIncisionDatetime() {
        return incisionDatetime;
    }
    public ProcedurePerformed setIncisionDatetime(DateTime incisionDatetime) {
        this.incisionDatetime = incisionDatetime;
        return this;
    }	
	
	
    @Child(name="negationRationale", order=9)
    private Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public ProcedurePerformed setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
	
	
    @Child(name="components", max=Child.MAX_UNLIMITED, order=10)
    private List<Component> components;
    public List<Component> getComponents() {
        return components;
    }
    public ProcedurePerformed setComponents(List<Code> components) {
        this.components = components;
        return this;
    }
	


}
