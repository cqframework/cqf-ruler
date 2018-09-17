package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.opencds.cqf.cql.runtime.DateTime;

import java.util.List;

@ResourceDef(name="PatientCharacteristicPayer", profile="TODO")
public class PatientCharacteristicPayer extends QdmBaseType {

    @Child(name="relevantPeriod", order=0)
    private Interval relevantPeriod;
    public Interval getRelevantPeriod() {
        return relevantPeriod;
    }
    public PatientCharacteristicPayer setRelevantPeriod(Interval relevantPeriod) {
        this.relevantPeriod = relevantPeriod;
        return this;
    }	
	

}
