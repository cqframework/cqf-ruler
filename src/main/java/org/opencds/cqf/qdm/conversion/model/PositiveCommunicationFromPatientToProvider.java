package org.opencds.cqf.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveCommunicationFromPatientToProvider", profile="TODO")
public class PositiveCommunicationFromPatientToProvider extends CommunicationFromPatientToProvider {
    @Override
    public PositiveCommunicationFromPatientToProvider copy() {
        PositiveCommunicationFromPatientToProvider retVal = new PositiveCommunicationFromPatientToProvider();
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
        return "PositiveCommunicationFromPatientToProvider";
    }
}
