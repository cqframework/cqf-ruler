package org.opencds.cqf.ruler.plugin.security;

import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.Coding;
import org.opencds.cqf.ruler.api.MetadataExtender;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.capability.RulerJpaCapabilityStatementProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class OAuthProvider implements MetadataExtender<CapabilityStatement> {

	@Autowired
	OAuthProperties oAuthProperties;

	@Override
	public void extend(CapabilityStatement metadata) {
		metadata.getRestFirstRep().getSecurity().setCors(oAuthProperties.getSecurityCors());
		Extension securityExtension = metadata.getRestFirstRep().getSecurity().addExtension();
		securityExtension.setUrl(oAuthProperties.getSecurityUrl());

		// security.extension.extension
		Extension securityExtExt = securityExtension.addExtension();
		securityExtExt.setUrl(oAuthProperties.getSecurityExtAuthUrl());
		securityExtExt.setValue(new UriType(oAuthProperties.getSecurityExtAuthValueUri()));
		Extension securityTokenExt = securityExtension.addExtension();
		securityTokenExt.setUrl(oAuthProperties.getSecurityExtTokenUrl());
		securityTokenExt.setValue(new UriType(oAuthProperties.getSecurityExtTokenValueUri()));

		// security.extension.service
		Coding coding = new Coding();
		coding.setSystem(oAuthProperties.getServiceSystem());
		coding.setCode(oAuthProperties.getServiceCode());
		coding.setDisplay(oAuthProperties.getServiceDisplay());
		CodeableConcept codeConcept = new CodeableConcept();
		codeConcept.addCoding(coding);
		metadata.getRestFirstRep().getSecurity().getService().add(codeConcept);
		// metadata.getRestFirstRep().getSecurity().getService() //how do we handle "text" on the sample not part of getService
	}
}




