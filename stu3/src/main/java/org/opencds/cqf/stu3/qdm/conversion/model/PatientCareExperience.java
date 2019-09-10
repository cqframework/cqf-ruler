package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.DateTimeType;

@ResourceDef(name="PatientCareExperience", profile="TODO")
public class PatientCareExperience extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    private DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public PatientCareExperience setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

    @Override
    public PatientCareExperience copy() {
        PatientCareExperience retVal = new PatientCareExperience();
        super.copyValues(retVal);
        retVal.authorDatetime = authorDatetime;
        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "PatientCareExperience";
    }
}