package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.qdm.QdmBaseType;


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


    @Override
    public PatientCharacteristicPayer copy() {
        PatientCharacteristicPayer retVal = new PatientCharacteristicPayer();
        super.copyValues(retVal);
        retVal.relevantPeriod = relevantPeriod;
        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PatientCharacteristicPayer";
    }

}
