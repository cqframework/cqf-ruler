package org.opencds.cqf.ruler.capability;

import java.util.List;


import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import jakarta.servlet.http.HttpServletRequest;
import org.opencds.cqf.ruler.api.MetadataExtender;

import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.JpaConformanceProviderDstu2;
import ca.uhn.fhir.model.dstu2.composite.MetaDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Conformance;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;

public class ExtensibleJpaConformanceProviderDstu2 extends JpaConformanceProviderDstu2 {

	List<MetadataExtender<Conformance>> myExtenders;

	public ExtensibleJpaConformanceProviderDstu2(RestfulServer theRestfulServer, IFhirSystemDao<Bundle, MetaDt> theSystemDao,
																JpaStorageSettings theJpaStorageSettings, List<MetadataExtender<Conformance>> theExtenders) {
		super(theRestfulServer, theSystemDao, theJpaStorageSettings);
		myExtenders = theExtenders;
	}

	@Override
	@Metadata
	public Conformance getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
		Conformance conformance = super.getServerConformance(theRequest, theRequestDetails);

		if (myExtenders != null) {
			for (MetadataExtender<Conformance> extender : myExtenders) {
				extender.extend(conformance);
			}
		}

		return conformance;
	}
}
