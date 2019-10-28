package org.opencds.cqf.dstu3.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.rest.server.IResourceProvider;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.opencds.cqf.cql.data.CompositeDataProvider;
import org.opencds.cqf.cql.type.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.type.Dstu3JpaFhirRetrieveProvider;

import java.util.Collection;

public class JpaDataProvider {//extends CompositeDataProvider {

	public Dstu3FhirModelResolver modelResolver;
	public Dstu3JpaFhirRetrieveProvider retrieveProvider;
	public CompositeDataProvider dataProvider;

    public JpaDataProvider(Collection<IResourceProvider> providers) {
		//super(new Dstu3FhirModelResolver(), new Dstu3JpaFhirRetrieveProvider(providers));
		FhirContext fhirContext = FhirContext.forDstu3();
		this.modelResolver = new Dstu3FhirModelResolver(fhirContext);
		this.retrieveProvider = new Dstu3JpaFhirRetrieveProvider(providers);
		this.dataProvider = new CompositeDataProvider(this.modelResolver, this.retrieveProvider);
	}
	
    public synchronized JpaResourceProviderDstu3<? extends IAnyResource> resolveResourceProvider(String datatype) {
		return this.retrieveProvider.resolveResourceProvider(datatype);
	}

}
