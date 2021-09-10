package org.opencds.cqf.dstu3.helpers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.opencds.cqf.cql.evaluator.cql2elm.model.CacheAwareModelManager;
import org.opencds.cqf.cql.evaluator.engine.execution.CacheAwareLibraryLoaderDecorator;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader;

import ca.uhn.fhir.cql.common.provider.LibraryContentProvider;
import ca.uhn.fhir.cql.common.provider.LibraryResolutionProvider;

public class LibraryHelper extends ca.uhn.fhir.cql.dstu3.helper.LibraryHelper {


    protected Map<VersionedIdentifier, Model> modelCache;
    protected Map<org.cqframework.cql.elm.execution.VersionedIdentifier, Library> libraryCache;
    protected CqlTranslatorOptions translatorOptions;

    public LibraryHelper(Map<VersionedIdentifier, Model> modelCache,
            Map<org.cqframework.cql.elm.execution.VersionedIdentifier, Library> libraryCache,
            CqlTranslatorOptions translatorOptions) {
        super(modelCache, libraryCache, translatorOptions);
        this.modelCache = modelCache;
        this.libraryCache = libraryCache;
        this.translatorOptions = translatorOptions;
    }


    public ModelManager getModelManager() {
        return new CacheAwareModelManager(this.modelCache);
    }

    public LibraryManager getLibraryManager(LibraryResolutionProvider<org.hl7.fhir.dstu3.model.Library> provider) {
		List<org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider> contentProviders = Collections.singletonList(new LibraryContentProvider<org.hl7.fhir.dstu3.model.Library, Attachment>(
			provider, x -> x.getContent(), x -> x.getContentType(), x -> x.getData()));
            
        LibraryManager libraryManager = new LibraryManager(this.getModelManager());
        for (org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider contentProvider : contentProviders) {
            libraryManager.getLibrarySourceLoader().registerProvider(contentProvider);
        }

        return libraryManager;
    } 

    public Library resolvePrimaryLibrary(PlanDefinition planDefinition,
            org.opencds.cqf.cql.engine.execution.LibraryLoader libraryLoader,
            LibraryResolutionProvider<org.hl7.fhir.dstu3.model.Library> libraryResourceProvider) {
        String id = planDefinition.getLibrary().get(0).getReference();

        Library library = resolveLibraryById(id, libraryLoader, libraryResourceProvider);

        if (library == null) {
            throw new IllegalArgumentException(String.format("Could not resolve primary library for PlanDefinition/%s",
                    planDefinition.getIdElement().getIdPart()));
        }

        return library;
    }


    @Override
    public org.opencds.cqf.cql.engine.execution.LibraryLoader createLibraryLoader(
            LibraryResolutionProvider<org.hl7.fhir.dstu3.model.Library> provider) {
        ModelManager modelManager = new CacheAwareModelManager(this.modelCache);
        LibraryManager libraryManager = new LibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().clearProviders();
        List<org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider> contentProviders = Collections
                .singletonList(new LibraryContentProvider<org.hl7.fhir.dstu3.model.Library, Attachment>(provider,
                        x -> x.getContent(), x -> x.getContentType(), x -> x.getData()));

        TranslatingLibraryLoader translatingLibraryLoader = new TranslatingLibraryLoader(modelManager, contentProviders,
                translatorOptions);

        return new CacheAwareLibraryLoaderDecorator(translatingLibraryLoader, libraryCache) {
            @Override
            protected Boolean translatorOptionsMatch(Library library) {
                return true;
            }
        };
    }

    // @Override
    // public org.opencds.cqf.cql.engine.execution.LibraryLoader createLibraryLoader(
    //         org.cqframework.cql.cql2elm.LibrarySourceProvider provider) {
    //     ModelManager modelManager = new CacheAwareModelManager(this.modelCache);
    //     LibraryManager libraryManager = new LibraryManager(modelManager);
    //     libraryManager.getLibrarySourceLoader().clearProviders();

    //     libraryManager.getLibrarySourceLoader().registerProvider(provider);

    //     TranslatingLibraryLoader translatingLibraryLoader = new TranslatingLibraryLoader(modelManager, null,
    //             translatorOptions);

    //     return new CacheAwareLibraryLoaderDecorator(translatingLibraryLoader, libraryCache) {
    //         @Override
    //         protected Boolean translatorOptionsMatch(Library library) {
    //             return true;
    //         }
    //     };
    // }
}
