package org.opencds.cqf.ruler.plugin.security.r4;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.UriType;
import org.opencds.cqf.ruler.api.MetadataExtender;
import org.opencds.cqf.ruler.plugin.security.SecurityProperties;
import org.springframework.beans.factory.annotation.Autowired;

public class OAuthProvider implements MetadataExtender<CapabilityStatement> {

	@Autowired
	SecurityProperties securityProperties;

	@Override
	public void extend(CapabilityStatement metadata) {
		metadata.getRestFirstRep().getSecurity().setCors(securityProperties.getOAuth().getSecurityCors());
		Extension securityExtension = metadata.getRestFirstRep().getSecurity().addExtension();
		securityExtension.setUrl(securityProperties.getOAuth().getSecurityUrl());

		// security.extension.extension
		Extension securityExtExt = securityExtension.addExtension();
		securityExtExt.setUrl(securityProperties.getOAuth().getSecurityExtAuthUrl());
		securityExtExt.setValue(new UriType(securityProperties.getOAuth().getSecurityExtAuthValueUri()));
		Extension securityTokenExt = securityExtension.addExtension();
		securityTokenExt.setUrl(securityProperties.getOAuth().getSecurityExtTokenUrl());
		securityTokenExt.setValue(new UriType(securityProperties.getOAuth().getSecurityExtTokenValueUri()));

		// security.extension.service
		Coding coding = new Coding();
		coding.setSystem(securityProperties.getOAuth().getServiceSystem());
		coding.setCode(securityProperties.getOAuth().getServiceCode());
		coding.setDisplay(securityProperties.getOAuth().getServiceDisplay());
		CodeableConcept codeConcept = new CodeableConcept();
		codeConcept.addCoding(coding);
		metadata.getRestFirstRep().getSecurity().getService().add(codeConcept);
		// metadata.getRestFirstRep().getSecurity().getService() //how do we handle "text" on the sample not part of getService
	}
}




