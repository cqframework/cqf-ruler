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

@ResourceDef(name="PositiveProcedurePerformed", profile="TODO")
public class PositiveProcedurePerformed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    private DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public PositiveProcedurePerformed setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

	
    @Child(name="relevantPeriod", order=1)
    private Interval relevantPeriod;
    public Interval getRelevantPeriod() {
        return relevantPeriod;
    }
    public PositiveProcedurePerformed setRelevantPeriod(Interval relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	
	

    @Child(name="reason", order=2)
    private Code reason;
    public Code getreason() {
        return reason;
    }
    public PositiveProcedurePerformed setreason(Code reason) {
        this.reason = reason;
        return this;
    }

	
    @Child(name="method", order=3)
    private Code method;
    public Code getmethod() {
        return method;
    }
    public PositiveProcedurePerformed setmethod(Code method) {
        this.method = method;
        return this;
    }	


    @Child(name="status", order=4)
    private Code status;
    public Code getstatus() {
        return status;
    }
    public PositiveProcedurePerformed setstatus(Code status) {
        this.status = status;
        return this;
    }		

	
    @Child(name="anatomicalApproachSite", order=5)
    private Code anatomicalApproachSite;
    public Code getanatomicalApproachSite() {
        return anatomicalApproachSite;
    }
    public PositiveProcedurePerformed setanatomicalApproachSite(Code anatomicalApproachSite) {
        this.anatomicalApproachSite = anatomicalApproachSite;
        return this;
    }

	
    @Child(name="anatomicalLocationSite", order=6)
    private Code anatomicalLocationSite;
    public Code getanatomicalLocationSite() {
        return anatomicalLocationSite;
    }
    public PositiveProcedurePerformed setanatomicalLocationSite(Code anatomicalLocationSite) {
        this.anatomicalLocationSite = anatomicalLocationSite;
        return this;
    }


    @Child(name="ordinality", order=7)
    private Code ordinality;
    public Code getordinality() {
        return ordinality;
    }
    public PositiveProcedurePerformed setordinality(Code ordinality) {
        this.ordinality = ordinality;
        return this;
    }	

    @Child(name="incisionDatetime", order=8)
    private DateTime incisionDatetime;
    public DateTime getincisionDatetime() {
        return incisionDatetime;
    }
    public PositiveProcedurePerformed setincisionDatetime(DateTime incisionDatetime) {
        this.incisionDatetime = incisionDatetime;
        return this;
    }	
	
	
    @Child(name="negationRationale", order=9)
    private Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public PositiveProcedurePerformed setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
	
	
    @Child(name="components", max=Child.MAX_UNLIMITED, order=10)
    private List<Component> components;
    public List<Component> getcomponents() {
        return components;
    }
    public PositiveProcedurePerformed setcomponents(List<Code> components) {
        this.components = components;
        return this;
    }
	

    @Override
    public PositiveProcedurePerformed copy() {
        PositiveProcedurePerformed retVal = new PositiveProcedurePerformed();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.relevantPeriod = relevantPeriod;
        retVal.reason = reason;
        retVal.status = status;
        retVal.anatomicalApproachSite = anatomicalApproachSite;
        retVal.ordinality = ordinality;
        retVal.incisionDatetime = incisionDatetime;
        retVal.negationRationale = negationRationale;
        retVal.components = components;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }
}
