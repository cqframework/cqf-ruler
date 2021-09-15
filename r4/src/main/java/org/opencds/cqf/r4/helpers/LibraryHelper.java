package org.opencds.cqf.r4.helpers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.Attachment;
import org.opencds.cqf.cql.evaluator.cql2elm.model.CacheAwareModelManager;

import ca.uhn.fhir.cql.common.provider.LibraryContentProvider;
import ca.uhn.fhir.cql.common.provider.LibraryResolutionProvider;

import org.opencds.cqf.cql.evaluator.engine.execution.CacheAwareLibraryLoaderDecorator;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader;

public class LibraryHelper extends ca.uhn.fhir.cql.r4.helper.LibraryHelper {

    protected Map<VersionedIdentifier, Model> modelCache;
    private Map<org.cqframework.cql.elm.execution.VersionedIdentifier, Library> libraryCache;
    private CqlTranslatorOptions translatorOptions;

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

    public LibraryManager getLibraryManager(LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> provider) {
        List<org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider> contentProviders = Collections
                .singletonList(new LibraryContentProvider<org.hl7.fhir.r4.model.Library, Attachment>(provider,
                        x -> x.getContent(), x -> x.getContentType(), x -> x.getData()));

        LibraryManager libraryManager = new LibraryManager(this.getModelManager());
        for (org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider contentProvider : contentProviders) {
            libraryManager.getLibrarySourceLoader().registerProvider(contentProvider);
        }

        return libraryManager;
    }

    public CqlTranslatorOptions getTranslatorOptions() {
        return this.translatorOptions;
    }

    public Map<org.cqframework.cql.elm.execution.VersionedIdentifier, Library> getLibraryCache() {
        return this.libraryCache;
    }

    @Override
    public org.opencds.cqf.cql.engine.execution.LibraryLoader createLibraryLoader(
            LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> provider) {
        ModelManager modelManager = new CacheAwareModelManager(this.modelCache);
        LibraryManager libraryManager = new LibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().clearProviders();
        List<org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider> contentProviders = Collections
                .singletonList(new LibraryContentProvider<org.hl7.fhir.r4.model.Library, Attachment>(provider,
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

    @Override
    public org.opencds.cqf.cql.engine.execution.LibraryLoader createLibraryLoader(
            org.cqframework.cql.cql2elm.LibrarySourceProvider provider) {
        ModelManager modelManager = new CacheAwareModelManager(this.modelCache);
        LibraryManager libraryManager = new LibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().clearProviders();

        libraryManager.getLibrarySourceLoader().registerProvider(provider);

        TranslatingLibraryLoader translatingLibraryLoader = new TranslatingLibraryLoader(modelManager, null,
                translatorOptions);

        return new CacheAwareLibraryLoaderDecorator(translatingLibraryLoader, libraryCache) {
            @Override
            protected Boolean translatorOptionsMatch(Library library) {
                return true;
            }
        };
    }

    public static InputStream extractContentStream(org.hl7.fhir.r4.model.Library library) {
        Attachment cql = null;
        for (Attachment a : library.getContent()) {
            if (a.getContentType().equals("text/cql")) {
                cql = a;
                break;
            }
        }

        if (cql == null) {
            return null;
        }
        return new ByteArrayInputStream(Base64.getDecoder().decode(cql.getDataElement().getValueAsString()));
    }
}
