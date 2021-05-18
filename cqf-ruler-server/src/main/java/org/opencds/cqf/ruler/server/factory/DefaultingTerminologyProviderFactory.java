package org.opencds.cqf.ruler.server.factory;

import java.util.Set;

import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;

import ca.uhn.fhir.context.FhirContext;

public class DefaultingTerminologyProviderFactory extends TerminologyProviderFactory {

    TerminologyProvider terminologyProvider;

    public DefaultingTerminologyProviderFactory(FhirContext fhirContext,
            Set<TypedTerminologyProviderFactory> terminologyProviderFactories, TerminologyProvider terminologyProvider) {
        super(fhirContext, terminologyProviderFactories);
        this.terminologyProvider = terminologyProvider;
    }

    @Override
    public TerminologyProvider create(EndpointInfo endpointInfo) {
        if (endpointInfo == null) {
            return this.terminologyProvider;
        }

        return super.create(endpointInfo);
    }
    
}
