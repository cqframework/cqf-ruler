
package org.opencds.cqf.dstu3.factories;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alphora.cql.service.factory.LibraryLoaderFactory;
import com.alphora.cql.service.loader.TranslatingLibraryLoader;
import com.alphora.cql.service.manager.CacheAwareModelManager;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.common.providers.LibraryResolutionProvider;
import org.opencds.cqf.common.providers.LibrarySourceProvider;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.common.evaluation.RulerLibraryLoader;

// This is intended to be general-purpose factory but there are specific cases where we probably want to change the behavior.
// For example, the global model cache will slow down (slightly) processing in the CLI mode, while it will speed up requests
// overall in service mode due to not needing to reload models on every request.
public class DefaultLibraryLoaderFactory implements LibraryLoaderFactory {

    private static final Map<VersionedIdentifier, Model> globalCache = new HashMap<>();
    private LibraryResolutionProvider<org.hl7.fhir.dstu3.model.Library> provider;

    public DefaultLibraryLoaderFactory(LibraryResolutionProvider<org.hl7.fhir.dstu3.model.Library> provider) {
        this.provider = provider;
    }

    public LibraryLoader create(List<String> libraries,
        EnumSet<CqlTranslator.Options> translatorOptions) {

        ModelManager modelManager = new CacheAwareModelManager(globalCache);

        LibraryManager libraryManager = createLibraryManager(modelManager, libraries, translatorOptions);
        LibraryLoader libraryLoader = new TranslatingLibraryLoader(libraryManager);

        return libraryLoader;
    }

    public LibraryLoader create(String libraryPath, EnumSet<CqlTranslator.Options> translatorOptions) {
        // TODO: At some point we might want to add support for a remote library URI
        List<String> libraries = this.getLibrariesFromPath(libraryPath);

        return this.create(libraries, translatorOptions);
    }

    public LibraryLoader create(LibraryResolutionProvider<org.hl7.fhir.dstu3.model.Library> provider) {
        ModelManager modelManager = new ModelManager();
        LibraryManager libraryManager = new LibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().clearProviders();
        
        libraryManager.getLibrarySourceLoader().registerProvider(
            new LibrarySourceProvider<org.hl7.fhir.dstu3.model.Library, org.hl7.fhir.dstu3.model.Attachment>(
                provider, 
                x -> x.getContent(),
                x -> x.getContentType(),
                x -> x.getData()));

        return new RulerLibraryLoader(libraryManager, modelManager);
    }

    List<String> getLibrariesFromPath(String path) {
        File directory = new File(path);
        File[] files = directory.listFiles((d, name) -> name.endsWith(".cql"));

        return Arrays.asList(files).stream().map(x -> x.toPath()).filter(Files::isRegularFile).map(t -> {
            try {
                return Files.readAllBytes(t);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        })
        .filter(x -> x != null)
        .map(x -> new String(x, StandardCharsets.UTF_8))
        .collect(Collectors.toList());
    }

    private LibraryManager createLibraryManager(ModelManager modelManager, List<String> libraries, EnumSet<CqlTranslator.Options> translatorOptions) {
        LibraryManager libraryManager = new LibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().clearProviders();
        
        libraryManager.getLibrarySourceLoader().registerProvider(
            new LibrarySourceProvider<org.hl7.fhir.dstu3.model.Library, org.hl7.fhir.dstu3.model.Attachment>(
                provider, 
                x -> x.getContent(),
                x -> x.getContentType(),
                x -> x.getData()));
        return libraryManager;
    }
}