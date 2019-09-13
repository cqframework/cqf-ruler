package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveCommunicationFromProviderToPatient", profile="TODO")
public class PositiveCommunicationFromProviderToPatient extends CommunicationFromProviderToPatient {
    @Override
    public PositiveCommunicationFromProviderToPatient copy() {
        PositiveCommunicationFromProviderToPatient retVal = new PositiveCommunicationFromProviderToPatient();
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
        return "PositiveCommunicationFromProviderToPatient";
    }
}
