package org.opencds.cqf.r4.providers;

import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.provider.r4.JpaConformanceProviderR4;
import ca.uhn.fhir.jpa.searchparam.registry.SearchParamRegistryImpl;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.hapi.rest.server.ServerCapabilityStatementProvider;
import org.hl7.fhir.r4.model.CapabilityStatement;

import javax.servlet.http.HttpServletRequest;

public class OAuthProvider extends ServerCapabilityStatementProvider {//JpaConformanceProviderR4 {
    /**
     *      This class is NOT designed to be a real OAuth provider.
     *      It is designed to provide a capability statement and to pass thru the path to the real oauth verification server.
     *      It should only get instantiated if hapi.properties has oauth.enabled set to true.
     */

    @Metadata
    @Override
    public CapabilityStatement getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
        CapabilityStatement retVal;
//        setIncludeResourceCounts(false);
//        setDaoConfig(new DaoConfig());      // we do NOT want a DAO inside the ruler
//        setSearchParamRegistry(new SearchParamRegistryImpl());
        setPublisher("Alphora");


        retVal = super.getServerConformance(theRequest, theRequestDetails);

        if(retVal != null){
            retVal.getImplementation().setDescription("cqf-ruler R4 server");
        }
        return retVal;
    }
}
