package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Coding;

@ResourceDef(name="FamilyHistory", profile="TODO")
public class FamilyHistory extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    private DateTimeType authorDatetime;
    public DateTimeType getAuthorDatetime() {
        return authorDatetime;
    }
    public FamilyHistory setAuthorDatetime(DateTimeType authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }
	
    @Child(name="relationship", order=1)
    private Coding relationship;
    public Coding getRelationship() {
        return relationship;
    }
    public FamilyHistory setRelationship(Coding relationship) {
        this.relationship = relationship;
        return this;
    }	

    @Override
    public FamilyHistory copy() {
        FamilyHistory retVal = new FamilyHistory();
        super.copyValues(retVal);
        retVal.authorDatetime = authorDatetime;
        retVal.relationship = relationship;
        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "FamilyHistory";
    }
}