package org.opencds.cqf.ruler.plugin.security;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.Coding;
import org.opencds.cqf.ruler.api.MetadataExtender;
import org.springframework.beans.factory.annotation.Autowired;

public class OAuthProvider implements MetadataExtender<CapabilityStatement> {

	@Autowired
	SecurityProperties oAuthProperties;

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




