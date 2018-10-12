package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.qdm.QdmBaseType;
import org.opencds.cqf.qdm.types.Id;

import java.util.List;

@ResourceDef(name="CommunicationFromProviderToPatient", profile="TODO")
public abstract class CommunicationFromProviderToPatient extends QdmBaseType {

    @Child(name="authorDatetime", order=0)
    DateTime authorDatetime;
    public DateTime getAuthorDatetime() {
        return authorDatetime;
    }
    public CommunicationFromProviderToPatient setAuthorDatetime(DateTime authorDatetime) {
        this.authorDatetime = authorDatetime;
        return this;
    }

	
    @Child(name="relatedTo", max=Child.MAX_UNLIMITED, order=1)
    List<Id> relatedTo;
    public List<Id> getRelatedTo() {
        return relatedTo;
    }
    public CommunicationFromProviderToPatient setRelatedTo(List<Id> relatedTo) {
        this.relatedTo = relatedTo;
        return this;
    }
	
	
    @Child(name="negationRationale", order=2)
    Code negationRationale;
    public Code getNegationRationale() {
        return negationRationale;
    }
    public CommunicationFromProviderToPatient setNegationRationale(Code negationRationale) {
        this.negationRationale = negationRationale;
        return this;
    }

	
}
