package org.opencds.cqf.ruler.server.factory;

import java.util.Set;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.builder.library.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;

import ca.uhn.fhir.context.FhirContext;

public class DefaultingLibraryLoaderFactory extends LibraryLoaderFactory {

    private LibraryLoader libraryLoader;

    public DefaultingLibraryLoaderFactory(FhirContext fhirContext, AdapterFactory adapterFactory,
            Set<TypedLibraryContentProviderFactory> libraryContentProviderFactories,
            LibraryVersionSelector libraryVersionSelector, LibraryLoader libraryLoader) {
        super(fhirContext, adapterFactory, libraryContentProviderFactories, libraryVersionSelector);
        this.libraryLoader = libraryLoader;
    }

    @Override
    public LibraryLoader create(EndpointInfo endpointInfo, CqlTranslatorOptions translatorOptions) {
        if (endpointInfo == null) {
            return this.libraryLoader;
        }

        return super.create(endpointInfo, translatorOptions);
    }
}