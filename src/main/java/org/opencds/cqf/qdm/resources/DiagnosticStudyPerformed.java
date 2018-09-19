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
import org.opencds.cqf.qdm.types.Component;

import java.util.List;

@ResourceDef(name="DiagnosticStudyPerformed", profile="TODO")
public abstract class DiagnosticStudyPerformed extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    private DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public DiagnosticStudyPerformed setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

	
    @Child(name="relevantPeriod", order=1)
    private Interval relevantPeriod;
    public Interval getRelevantPeriod() {
        return relevantPeriod;
    }
    public DiagnosticStudyPerformed setRelevantPeriod(Interval relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	
	

    @Child(name="reason", order=2)
    private Code reason;
    public Code getReason() {
        return reason;
    }
    public DiagnosticStudyPerformed setReason(Code reason) {
        this.reason = reason;
        return this;
    }

	
    @Child(name="resultDatetime", order=3)
    private DateTime resultDatetime;
    public DateTime getResultDatetime() {
        return resultDatetime;
    }
    public DiagnosticStudyPerformed setResultDatetime(DateTime resultDatetime) {
        this.resultDatetime = resultDatetime;
        return this;
    }	
	

    @Child(name="status", order=4)
    private Code status;
    public Code getStatus() {
        return status;
    }
    public DiagnosticStudyPerformed setStatus(Code status) {
        this.status = status;
        return this;
    }		

	
    @Child(name="method", order=5)
    private Code method;
    public Code getMethod() {
        return method;
    }
    public DiagnosticStudyPerformed setMethod(Code method) {
        this.method = method;
        return this;
    }	


    @Child(name="facilityLocation", order=6)
    private Code facilityLocation;
    public Code getFacilityLocation() {
        return facilityLocation;
    }
    public DiagnosticStudyPerformed setFacilityLocation(Code facilityLocation) {
        this.facilityLocation = facilityLocation;
        return this;
    }	
	
	
    @Child(name="negationRationale", order=7)
    private Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public DiagnosticStudyPerformed setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }
	
	
    @Child(name="components", max=Child.MAX_UNLIMITED, order=8)
    private List<Component> components;
    public List<Component> getComponents() {
        return components;
    }
    public DiagnosticStudyPerformed setComponents(List<Component> components) {
        this.components = components;
        return this;
    }

}
