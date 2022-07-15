package org.opencds.cqf.ruler.cpg.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.PrimitiveType;
import org.hl7.fhir.instance.model.api.IBaseBooleanDatatype;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.cql.engine.fhir.terminology.Dstu3FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.ruler.cpg.LibraryUser;
import org.opencds.cqf.ruler.cql.JpaLibraryContentProviderFactory;
import org.opencds.cqf.ruler.cql.JpaTerminologyProviderFactory;
import org.opencds.cqf.ruler.cql.LibraryLoaderFactory;
import org.opencds.cqf.ruler.utility.Clients;

import java.util.List;
import java.util.stream.Collectors;

public class STU3LibraryUser extends LibraryUser {
	public STU3LibraryUser(DaoRegistry daoRegistry, FhirContext fhirContext, ModelResolver myModelResolver, LibraryLoaderFactory libraryLoaderFactory, JpaLibraryContentProviderFactory jpaLibraryContentProviderFactory, FhirRestLibraryContentProviderFactory fhirRestLibraryContentProviderFactory, JpaTerminologyProviderFactory jpaTerminologyProviderFactory, AdapterFactory adapterFactory, String subject, List<String> expression, IBaseParameters parameters, IBaseBooleanDatatype useServerData, IBaseBundle data, IDomainResource dataEndpoint, IDomainResource libraryContentEndpoint, IDomainResource terminologyEndpoint, String content) {
		super(daoRegistry, fhirContext, myModelResolver, libraryLoaderFactory, jpaLibraryContentProviderFactory, fhirRestLibraryContentProviderFactory, jpaTerminologyProviderFactory, adapterFactory, subject, expression, parameters, useServerData, data, dataEndpoint, libraryContentEndpoint, terminologyEndpoint, content);
	}

	@Override
	public void resolveLibraryContentEndpoint(List<LibraryContentProvider> libraryProviders) {
		if (getLibraryContentEndpoint() != null) {
			libraryProviders.add(
				getFhirRestLibraryContentProviderFactory().create(
					((Endpoint) getLibraryContentEndpoint()).getAddress(),
					((Endpoint) getLibraryContentEndpoint()).getHeader().stream().map(PrimitiveType::asStringValue).collect(Collectors.toList())
				)
			);
		}
	}

	@Override
	public TerminologyProvider resolveTerminologyProvider(RequestDetails requestDetails) {
		return getTerminologyEndpoint() != null
			? new Dstu3FhirTerminologyProvider(Clients.forEndpoint(getFhirContext(), ((Endpoint) getTerminologyEndpoint())))
			: getJpaTerminologyProviderFactory().create(requestDetails);
	}

	@Override
	public IGenericClient getDataEndpointClient() {
		return Clients.forEndpoint(((Endpoint) getDataEndpoint()));
	}

	@Override
	public boolean isTerminologyEndpointSameAsData() {
		return ((Endpoint) getTerminologyEndpoint()).getAddress().equals(((Endpoint) getDataEndpoint()).getAddress());
	}
}
