package org.opencds.cqf.ruler.capability;

import java.util.List;


import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.opencds.cqf.ruler.api.MetadataExtender;

import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.JpaCapabilityStatementProvider;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;

public class ExtensibleJpaCapabilityStatementProvider extends JpaCapabilityStatementProvider {

	private List<MetadataExtender<IBaseConformance>> myExtenders;

	public ExtensibleJpaCapabilityStatementProvider(RestfulServer theRestfulServer, IFhirSystemDao<?, ?> theSystemDao,
																	JpaStorageSettings theJpaStorageSettings, ISearchParamRegistry theSearchParamRegistry,
																	IValidationSupport theValidationSupport, List<MetadataExtender<IBaseConformance>> theExtenders) {
		super(theRestfulServer, theSystemDao, theJpaStorageSettings, theSearchParamRegistry, theValidationSupport);
		myExtenders = theExtenders;
	}

	@Override
	@Metadata
	public IBaseConformance getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
		IBaseConformance conformance = super.getServerConformance(theRequest, theRequestDetails);

		if (myExtenders != null) {
			for (MetadataExtender<IBaseConformance> extender : myExtenders) {
				extender.extend(conformance);
			}
		}

		return conformance;
	}
}
