package org.opencds.cqf.dstu3.evaluation;

import org.opencds.cqf.config.ResourceProviderRegistry;
import org.opencds.cqf.cql.data.CompositeDataProvider;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.dstu3.providers.ApelonFhirTerminologyProvider;
import org.opencds.cqf.qdm.model.Qdm54ModelResolver;
import org.opencds.cqf.qdm.providers.Qdm54RetrieveProvider;
import org.opencds.cqf.retrieve.JpaFhirRetrieveProvider;

import ca.uhn.fhir.context.FhirContext;

// This class is a relatively dumb factory for data providers. It supports only
// creating JPA providers for FHIR and QDM, and only basic auth for terminology
public class ProviderFactory {

    ResourceProviderRegistry registry;
    TerminologyProvider defaultTerminologyProvider;
    FhirContext fhirContext;

    public ProviderFactory(FhirContext fhirContext, ResourceProviderRegistry registry,
            TerminologyProvider defaultTerminologyProvider) {
        this.defaultTerminologyProvider = defaultTerminologyProvider;
        this.registry = registry;
        this.fhirContext = fhirContext;
    }

    public DataProvider createDataProvider(String model, String version) {
        return this.createDataProvider(model, version, null, null, null);
    }

    public DataProvider createDataProvider(String model, String version, String url, String user, String pass) {
        TerminologyProvider terminologyProvider = this.createTerminologyProvider(url, user, pass);
        return this.createDataProvider(model, version, terminologyProvider);
    }

    public DataProvider createDataProvider(String model, String version, TerminologyProvider terminologyProvider) {
        if (model.equals("FHIR") && version.equals("3.0.0")) {
            Dstu3FhirModelResolver modelResolver = new Dstu3FhirModelResolver(this.fhirContext);
            JpaFhirRetrieveProvider retrieveProvider = new JpaFhirRetrieveProvider(this.registry);
            retrieveProvider.setTerminologyProvider(terminologyProvider);
            retrieveProvider.setExpandValueSets(true);

            return new CompositeDataProvider(modelResolver, retrieveProvider);
        } else if (model.equals("QDM") && version.equals("5.4")) {
            Qdm54RetrieveProvider retrieveProvider = new Qdm54RetrieveProvider(terminologyProvider);
            return new CompositeDataProvider(new Qdm54ModelResolver(), retrieveProvider);
        }

        throw new IllegalArgumentException(
                String.format("Can't construct a data provider for model %s version %s", model, version));
    }

    public TerminologyProvider createTerminologyProvider(String url, String user, String pass) {
        if (url != null && !url.isEmpty()) {
            if (url.contains("apelon.com")) {
                return new ApelonFhirTerminologyProvider().withBasicAuth(user, pass).setEndpoint(url, false);
            } else {
                return new FhirTerminologyProvider().withBasicAuth(user, pass).setEndpoint(url, false);
            }
        } else
            return this.defaultTerminologyProvider;
    }
}