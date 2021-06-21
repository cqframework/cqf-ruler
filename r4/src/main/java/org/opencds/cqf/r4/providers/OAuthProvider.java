package org.opencds.cqf.r4.providers;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.UriType;
import org.opencds.cqf.common.config.HapiProperties;

import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;

/**
 *      This class is NOT designed to be a real OAuth provider.
 *      It is designed to provide a capability statement and to pass thru the path to the real oauth verification server.
 *      It should only get instantiated if hapi.properties has oauth.enabled set to true.
 */
public class OAuthProvider extends CqfRulerJpaConformanceProviderR4 {
    public OAuthProvider(RestfulServer theRestfulServer, IFhirSystemDao<?, ?> theSystemDao, DaoConfig theDaoConfig,
            ISearchParamRegistry theSearchParamRegistry, IValidationSupport theValidationSupport) {
        super(theRestfulServer, theSystemDao, theDaoConfig, theSearchParamRegistry, theValidationSupport);
    }

    @Metadata
    @Override
    public CapabilityStatement getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
        CapabilityStatement retVal = (CapabilityStatement)super.getServerConformance(theRequest, theRequestDetails);

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
