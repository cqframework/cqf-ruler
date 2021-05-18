package org.opencds.cqf.ruler.server.factory;

import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;

import ca.uhn.fhir.context.FhirContext;

public class DefaultingDataProviderFactory extends DataProviderFactory {

    private Triple<String, ModelResolver, RetrieveProvider> dataProvider;

    public DefaultingDataProviderFactory(FhirContext fhirContext, Set<ModelResolverFactory> modelResolverFactories,
            Set<TypedRetrieveProviderFactory> retrieveProviderFactories, Triple<String, ModelResolver, RetrieveProvider> dataProvider) {
        super(fhirContext, modelResolverFactories, retrieveProviderFactories);
        this.dataProvider = dataProvider;
    }

    @Override
    public Triple<String, ModelResolver, RetrieveProvider> create(EndpointInfo endpointInfo) {
        if (endpointInfo == null) {
            return this.dataProvider;
        }

        return super.create(endpointInfo);
    }
    
}
