package org.opencds.cqf.dstu3.qdm.conversion.model;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="PositiveCommunicationFromProviderToProvider", profile="TODO")
public class PositiveCommunicationFromProviderToProvider extends CommunicationFromProviderToProvider {
    @Override
    public PositiveCommunicationFromProviderToProvider copy() {
        PositiveCommunicationFromProviderToProvider retVal = new PositiveCommunicationFromProviderToProvider();
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
        return "PositiveCommunicationFromProviderToProvider";
    }
}
