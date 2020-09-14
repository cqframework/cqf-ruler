package org.opencds.cqf.dstu3.providers;

import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.jpa.searchparam.registry.ISearchParamRegistry;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.hl7.fhir.dstu3.model.*;
import org.opencds.cqf.common.config.HapiProperties;

import javax.servlet.http.HttpServletRequest;

public class OAuthProvider extends JpaConformanceProviderDstu3 {
    public OAuthProvider(RestfulServer theRestfulServer, IFhirSystemDao<Bundle, Meta> theSystemDao, DaoConfig theDaoConfig, ISearchParamRegistry theSearchParamRegistry) {
        super(theRestfulServer, theSystemDao, theDaoConfig, theSearchParamRegistry);
    }

    @Metadata
    @Override
    public CapabilityStatement getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
        CapabilityStatement retVal;
        retVal = super.getServerConformance(theRequest, theRequestDetails);

        retVal.getRestFirstRep().getSecurity().setCors(HapiProperties.getOauthSecurityCors());
        Extension securityExtension = retVal.getRestFirstRep().getSecurity().addExtension();
        securityExtension.setUrl(HapiProperties.getOauthSecurityUrl());
        // security.extension.extension
        Extension securityExtExt = securityExtension.addExtension();
        securityExtExt.setUrl(HapiProperties.getOauthSecurityExtAuthUrl());
        securityExtExt.setValue(new UriType(HapiProperties.getOauthSecurityExtAuthValueUri()));
        Extension securityTokenExt = securityExtension.addExtension();
        securityTokenExt.setUrl(HapiProperties.getOauthSecurityExtTokenUrl());
        securityTokenExt.setValue(new UriType(HapiProperties.getOauthSecurityExtTokenValueUri()));

        // security.extension.service
        Coding coding = new Coding();
        coding.setSystem(HapiProperties.getOauthServiceSystem());
        coding.setCode(HapiProperties.getOauthServiceCode());
        coding.setDisplay(HapiProperties.getOauthServiceDisplay());
        CodeableConcept codeConcept = new CodeableConcept();
        codeConcept.addCoding(coding);
        retVal.getRestFirstRep().getSecurity().getService().add(codeConcept);
        // retVal.getRestFirstRep().getSecurity().getService() //how do we handle "text" on the sample not part of getService

        return retVal;
    }

}
