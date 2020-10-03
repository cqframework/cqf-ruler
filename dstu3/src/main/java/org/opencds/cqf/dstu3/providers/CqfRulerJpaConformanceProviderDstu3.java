package org.opencds.cqf.dstu3.providers;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.StringType;
import org.opencds.cqf.dstu3.servlet.BaseServlet;

import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class CqfRulerJpaConformanceProviderDstu3 extends JpaConformanceProviderDstu3 {

    @Metadata
    @Override
    public CapabilityStatement getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
        CapabilityStatement retVal;
        retVal = super.getServerConformance(theRequest, theRequestDetails);

        Extension softwareModuleExtension = new Extension().setUrl("http://hl7.org/fhir/StructureDefinition/capabilitystatement-softwareModule");
        Extension softwareModuleNameExtension = new Extension().setUrl("name").setValue(new StringType("CQF Ruler FHIR DSTU3 Server"));
        Extension softwareModuleVersionExtension = new Extension().setUrl("version").setValue(new StringType(BaseServlet.class.getPackage().getImplementationVersion()));
        softwareModuleExtension.addExtension(softwareModuleNameExtension);
        softwareModuleExtension.addExtension(softwareModuleVersionExtension);
        retVal.getSoftware().addExtension(softwareModuleExtension);

        return retVal;
    }
}