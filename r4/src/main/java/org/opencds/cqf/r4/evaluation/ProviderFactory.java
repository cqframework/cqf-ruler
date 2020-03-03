package org.opencds.cqf.r4.evaluation;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.opencds.cqf.common.helpers.ClientHelper;
import org.opencds.cqf.common.providers.Dstu3ApelonFhirTerminologyProvider;
import org.opencds.cqf.cql.data.CompositeDataProvider;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.model.R4FhirModelResolver;
import org.opencds.cqf.cql.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.Dstu3FhirTerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.R4FhirTerminologyProvider;
import org.opencds.cqf.common.evaluation.EvaluationProviderFactory;
import org.opencds.cqf.common.providers.R4ApelonFhirTerminologyProvider;
import org.opencds.cqf.common.retrieve.JpaFhirRetrieveProvider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.registry.ISearchParamRegistry;

// This class is a relatively dumb factory for data providers. It supports only
// creating JPA providers for FHIR, and only basic auth for terminology
public class ProviderFactory implements EvaluationProviderFactory {

    DaoRegistry registry;
    TerminologyProvider defaultTerminologyProvider;
    FhirContext fhirContext;

    public ProviderFactory(FhirContext fhirContext, DaoRegistry registry,
            TerminologyProvider defaultTerminologyProvider) {
        this.defaultTerminologyProvider = defaultTerminologyProvider;
        this.registry = registry;
        this.fhirContext = fhirContext;
    }

    public DataProvider createDataProvider(String model, String version) {
        return this.createDataProvider(model, version, null, null, null);
    }

    public DataProvider createDataProvider(String model, String version, String url, String user, String pass) {
        TerminologyProvider terminologyProvider = this.createTerminologyProvider(model, version, url, user, pass);
        return this.createDataProvider(model, version, terminologyProvider);
    }

    public DataProvider createDataProvider(String model, String version, TerminologyProvider terminologyProvider) {
        if (model.equals("FHIR") && version.equals("4.0.0")) {
            R4FhirModelResolver modelResolver = new R4FhirModelResolver();
            JpaFhirRetrieveProvider retrieveProvider = new JpaFhirRetrieveProvider(this.registry, new SearchParameterResolver(this.fhirContext));
            retrieveProvider.setTerminologyProvider(terminologyProvider);
            retrieveProvider.setExpandValueSets(true);

            return new CompositeDataProvider(modelResolver, retrieveProvider);
        }

        throw new IllegalArgumentException(
                String.format("Can't construct a data provider for model %s version %s", model, version));
    }

    public TerminologyProvider createTerminologyProvider(String model, String version, String url, String user, String pass) {
        if(url != null && !url.isEmpty()){
            IGenericClient client = ClientHelper.getClient(FhirContext.forR4(), url, user, pass);
            if (url.contains("apelon.com")) {
                return new R4ApelonFhirTerminologyProvider(client);
            }
            return new R4FhirTerminologyProvider(client);
        }
        return this.defaultTerminologyProvider;
    }
}