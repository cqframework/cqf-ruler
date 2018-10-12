package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.qdm.QdmBaseType;


@ResourceDef(name="EncounterRecommended", profile="TODO")
public abstract class EncounterRecommended extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public EncounterRecommended setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

	
    @Child(name="reason", order=1)
    Code reason;
    public Code getReason() {
        return reason;
    }
    public EncounterRecommended setReason(Code reason) {
        this.reason = reason;
        return this;
    }


    @Child(name="facilityLocation", order=2)
    Code facilityLocation;
    public Code getFacilityLocation() {
        return facilityLocation;
    }
    public EncounterRecommended setFacilityLocation(Code facilityLocation) {
        this.facilityLocation = facilityLocation;
        return this;
    }

	
    @Child(name="negationRationale", order=3)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public EncounterRecommended setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

	




	
}
