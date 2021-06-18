package org.opencds.cqf.r4.providers;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.r4.servlet.BaseServlet;

import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.JpaCapabilityStatementProvider;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;

public class CqfRulerJpaConformanceProviderR4 extends JpaCapabilityStatementProvider {
    public CqfRulerJpaConformanceProviderR4(RestfulServer theRestfulServer, IFhirSystemDao<?, ?> theSystemDao,
            DaoConfig theDaoConfig, ISearchParamRegistry theSearchParamRegistry,
            IValidationSupport theValidationSupport) {
        super(theRestfulServer, theSystemDao, theDaoConfig, theSearchParamRegistry, theValidationSupport);
    }

    @Metadata
    @Override
    public IBaseConformance getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
        CapabilityStatement retVal;
        retVal = (CapabilityStatement)super.getServerConformance(theRequest, theRequestDetails);

        Extension softwareModuleExtension = new Extension().setUrl("http://hl7.org/fhir/StructureDefinition/capabilitystatement-softwareModule");
        Extension softwareModuleNameExtension = new Extension().setUrl("name").setValue(new StringType("CQF Ruler FHIR R4 Server"));
        Extension softwareModuleVersionExtension = new Extension().setUrl("version").setValue(new StringType(BaseServlet.class.getPackage().getImplementationVersion()));
        softwareModuleExtension.addExtension(softwareModuleNameExtension);
        softwareModuleExtension.addExtension(softwareModuleVersionExtension);
        retVal.getSoftware().addExtension(softwareModuleExtension);

        return retVal;
    }
}