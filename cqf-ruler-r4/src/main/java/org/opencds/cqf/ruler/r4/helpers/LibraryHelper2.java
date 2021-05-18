package org.opencds.cqf.ruler.r4.helpers;

import java.util.Map;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.fhir.r4.model.Attachment;
import org.opencds.cqf.cql.evaluator.cql2elm.model.CacheAwareModelManager;

import ca.uhn.fhir.cql.common.evaluation.LibraryLoader;
import ca.uhn.fhir.cql.common.provider.LibraryResolutionProvider;
import ca.uhn.fhir.cql.common.provider.LibrarySourceProvider;
/**
 * Created by Christopher on 1/11/2017.
 */
public class LibraryHelper2 extends ca.uhn.fhir.cql.r4.helper.LibraryHelper {

    Map<org.hl7.elm.r1.VersionedIdentifier, Model> modelCache;

    public LibraryHelper2(Map<org.hl7.elm.r1.VersionedIdentifier, Model> modelCache) {
        super(modelCache);

        this.modelCache = modelCache;
    }

    @Override
    public org.opencds.cqf.cql.engine.execution.LibraryLoader createLibraryLoader(LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> provider) {
		ModelManager modelManager = new CacheAwareModelManager(this.modelCache);
        LibraryManager libraryManager = new LibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().clearProviders();

        libraryManager.getLibrarySourceLoader().registerProvider(
                new LibrarySourceProvider<org.hl7.fhir.r4.model.Library, Attachment>(provider,
                        x -> x.getContent(), x -> x.getContentType(), x -> x.getData()));

        return new LibraryLoader(libraryManager, modelManager);
    }

    @Override
    public org.opencds.cqf.cql.engine.execution.LibraryLoader createLibraryLoader(org.cqframework.cql.cql2elm.LibrarySourceProvider provider) {
        ModelManager modelManager = new CacheAwareModelManager(this.modelCache);
        LibraryManager libraryManager = new LibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().clearProviders();

        libraryManager.getLibrarySourceLoader().registerProvider(provider);

        return new LibraryLoader(libraryManager, modelManager);
    }
}
