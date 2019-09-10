package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativeCommunicationFromPatientToProvider", profile="TODO")
public class NegativeCommunicationFromPatientToProvider extends CommunicationFromPatientToProvider {
    @Override
    public NegativeCommunicationFromPatientToProvider copy() {
        NegativeCommunicationFromPatientToProvider retVal = new NegativeCommunicationFromPatientToProvider();
        super.copyValues(retVal);

        retVal.authorDatetime = authorDatetime;
        retVal.relatedTo = relatedTo;
        retVal.negationRationale = negationRationale;

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "NegativeCommunicationFromPatientToProvider";
    }
}
