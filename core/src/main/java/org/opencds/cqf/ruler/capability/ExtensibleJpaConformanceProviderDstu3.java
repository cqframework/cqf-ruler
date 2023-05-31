package org.opencds.cqf.ruler.capability;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Meta;
import org.opencds.cqf.ruler.api.MetadataExtender;

import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;

public class ExtensibleJpaConformanceProviderDstu3 extends JpaConformanceProviderDstu3 {

	List<MetadataExtender<CapabilityStatement>> myExtenders;

	public ExtensibleJpaConformanceProviderDstu3(RestfulServer theRestfulServer, IFhirSystemDao<Bundle, Meta> theSystemDao,
																JpaStorageSettings theStorageSettings, ISearchParamRegistry theSearchParamRegistry,
																List<MetadataExtender<CapabilityStatement>> theExtenders) {
		super(theRestfulServer, theSystemDao, theStorageSettings, theSearchParamRegistry);
		myExtenders = theExtenders;
	}

	@Override
	@Metadata
	public CapabilityStatement getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
		CapabilityStatement conformance = super.getServerConformance(theRequest, theRequestDetails);

		if (myExtenders != null) {
			for (MetadataExtender<CapabilityStatement> extender : myExtenders) {
				extender.extend(conformance);
			}
		}

		return conformance;
	}
}
