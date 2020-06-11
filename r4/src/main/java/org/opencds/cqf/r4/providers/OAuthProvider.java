package org.opencds.cqf.r4.providers;

import ca.uhn.fhir.jpa.provider.r4.JpaConformanceProviderR4;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.CapabilityStatement;

import javax.servlet.http.HttpServletRequest;

public class OAuthProvider extends JpaConformanceProviderR4 {

    @Override
    public CapabilityStatement getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
        CapabilityStatement retVal;

        retVal = super.getServerConformance(theRequest, theRequestDetails);

        if(retVal != null){
            retVal.getImplementation().setDescription("cqf-ruler R4 server");
        }
        return retVal;
    }
}
