package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Quantity;
import org.opencds.cqf.qdm.QdmBaseType;
import org.opencds.cqf.qdm.types.FacilityLocation;

import java.util.List;

public abstract class LaboratoryTestPerformed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public LaboratoryTestPerformed setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }


    @Child(name="relevantPeriod", order=1)
    Interval relevantPeriod;
    public Interval getRelevantPeriod() {
        return relevantPeriod;
    }
    public LaboratoryTestPerformed setRelevantPeriod(Interval relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	
	
	
    @Child(name="status", order=2)
    Code status;
    public Code getStatus() {
        return status;
    }
    public LaboratoryTestPerformed setStatus(Code status) {
        this.status = status;
        return this;
    }	
	
	
    @Child(name="method", order=3)
    Code method;
    public Code getMethod() {
        return method;
    }
    public LaboratoryTestPerformed setMethod(Code method) {
        this.method = method;
        return this;
    }		
	
	
    @Child(name="resultDatetime", order=4)
    DateTime resultDatetime;
    public DateTime getResultDatetime() {
        return resultDatetime;
    }
    public LaboratoryTestPerformed setResultDatetime(DateTime resultDatetime) {
        this.resultDatetime = resultDatetime;
        return this;
    }	
	
	
    @Child(name="reason", order=5)
    Code reason;
    public Code getReason() {
        return reason;
    }
    public LaboratoryTestPerformed setReason(Code reason) {
        this.reason = reason;
        return this;
    }	
	
	
    @Child(name="referenceRange", order=6)
    private Interval referenceRange;
    public Interval getReferenceRange() {
        return referenceRange;
    }
    public LaboratoryTestPerformed setReferenceRange(Interval referenceRange) {
        this.referenceRange = referenceRange;
        return this;
    }	
		
	
    @Child(name="negationRationale", order=7)
    private Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public LaboratoryTestPerformed setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
	
	
    @Child(name="components", max=Child.MAX_UNLIMITED, order=8)
    private List<ResultComponent> components;
    public List<ResultComponent> getComponents() {
        return components;
    }
    public LaboratoryTestPerformed setComponents(List<ResultComponent> components) {
        this.components = components;
        return this;
    }	
	
	
}
