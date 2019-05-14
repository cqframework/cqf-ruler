package org.opencds.cqf.helpers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.xml.bind.JAXBException;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.cqframework.cql.elm.tracking.TrackBack;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.config.NonCachingLibraryManager;
import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.config.STU3LibrarySourceProvider;
import org.opencds.cqf.cql.execution.CqlLibraryReader;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;

/**
 * Created by Christopher on 1/11/2017.
 */
public class LibraryHelper {

    public static Library readLibrary(InputStream xmlStream) {
        try {
            return CqlLibraryReader.read(xmlStream);
        } catch (IOException | JAXBException e) {
            throw new IllegalArgumentException("Error encountered while reading ELM xml: " + e.getMessage());
        }
    }

    public static STU3LibraryLoader createLibraryLoader(LibraryResourceProvider provider) {
        ModelManager modelManager = new ModelManager();
        LibraryManager libraryManager = new NonCachingLibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().clearProviders();
        libraryManager.getLibrarySourceLoader().registerProvider(new STU3LibrarySourceProvider(provider));
        return new STU3LibraryLoader(provider, libraryManager, modelManager);
    }

    public static String errorsToString(Iterable<CqlTranslatorException> exceptions) {
        ArrayList<String> errors = new ArrayList<>();
        for (CqlTranslatorException error : exceptions) {
            TrackBack tb = error.getLocator();
            String lines = tb == null ? "[n/a]" : String.format("%s[%d:%d, %d:%d]",
                    (tb.getLibrary() != null ? tb.getLibrary().getId() + (tb.getLibrary().getVersion() != null
                            ? ("-" + tb.getLibrary().getVersion()) : "") : ""),
                    tb.getStartLine(), tb.getStartChar(), tb.getEndLine(), tb.getEndChar());
            errors.add(lines + error.getMessage());
        }

        return errors.toString();
    }

    public static CqlTranslator getTranslator(String cql, LibraryManager libraryManager, ModelManager modelManager) {
        return getTranslator(new ByteArrayInputStream(cql.getBytes(StandardCharsets.UTF_8)), libraryManager, modelManager);
    }

    public static CqlTranslator getTranslator(InputStream cqlStream, LibraryManager libraryManager, ModelManager modelManager) {
        ArrayList<CqlTranslator.Options> options = new ArrayList<>();
//        options.add(CqlTranslator.Options.EnableDateRangeOptimization);
        options.add(CqlTranslator.Options.EnableAnnotations);
        options.add(CqlTranslator.Options.EnableDetailedErrors);
        CqlTranslator translator;
        try {
            translator = CqlTranslator.fromStream(cqlStream, modelManager, libraryManager,
                    options.toArray(new CqlTranslator.Options[options.size()]));
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Errors occurred translating library: %s", e.getMessage()));
        }

        if (translator.getErrors().size() > 0) {
            throw new IllegalArgumentException(errorsToString(translator.getErrors()));
        }

        return translator;
    }

    public static Library translateLibrary(String cql, LibraryManager libraryManager, ModelManager modelManager) {
        return translateLibrary(new ByteArrayInputStream(cql.getBytes(StandardCharsets.UTF_8)), libraryManager, modelManager);
    }

    public static Library translateLibrary(InputStream cqlStream, LibraryManager libraryManager, ModelManager modelManager) {
        CqlTranslator translator = getTranslator(cqlStream, libraryManager, modelManager);
        return readLibrary(new ByteArrayInputStream(translator.toXml().getBytes(StandardCharsets.UTF_8)));
    }

    public static Library translateLibrary(CqlTranslator translator) {
        return readLibrary(new ByteArrayInputStream(translator.toXml().getBytes(StandardCharsets.UTF_8)));
    }

    public static void loadLibraries(Measure measure, STU3LibraryLoader libraryLoader, LibraryResourceProvider libraryResourceProvider)
    {
        // clear library cache
        libraryLoader.getLibraries().clear();

        // load libraries
        for (Reference ref : measure.getLibrary()) {
            // if library is contained in measure, load it into server
            if (ref.getReferenceElement().getIdPart().startsWith("#")) {
                for (Resource resource : measure.getContained()) {
                    if (resource instanceof org.hl7.fhir.dstu3.model.Library
                            && resource.getIdElement().getIdPart().equals(ref.getReferenceElement().getIdPart().substring(1)))
                    {
                        libraryResourceProvider.getDao().update((org.hl7.fhir.dstu3.model.Library) resource);
                    }
                }
            }

            // We just loaded it into the server so we can access it by Id
            String id = ref.getReferenceElement().getIdPart();
            if (id.startsWith("#")) {
                id = id.substring(1);
            }

            org.hl7.fhir.dstu3.model.Library library = LibraryResourceHelper.resolveLibraryById(libraryLoader.getLibraryResourceProvider(), id);
            libraryLoader.load(new VersionedIdentifier().withId(library.getName()).withVersion(library.getVersion()));
        }

        for (RelatedArtifact artifact : measure.getRelatedArtifact()) {
            if (artifact.hasType() && artifact.getType().equals(RelatedArtifactType.DEPENDSON) && artifact.hasResource() && artifact.getResource().hasReference()) {
                org.hl7.fhir.dstu3.model.Library library = LibraryResourceHelper.resolveLibraryById(libraryLoader.getLibraryResourceProvider(), artifact.getResource().getReferenceElement().getIdPart());
                libraryLoader.load(new VersionedIdentifier().withId(library.getName()).withVersion(library.getVersion()));
            }
        }

        if (libraryLoader.getLibraries().isEmpty()) {
            throw new IllegalArgumentException(String
                    .format("Could not load library source for libraries referenced in Measure/%s.", measure.getId()));
        }
    }

    public static Library resolvePrimaryLibrary(Measure measure, STU3LibraryLoader libraryLoader)
    {
        // default is the first library reference
        String id = measure.getLibraryFirstRep().getReferenceElement().getIdPart();

        Library library = null;

        org.hl7.fhir.dstu3.model.Library fhirLibrary = LibraryResourceHelper.resolveLibraryById(libraryLoader.getLibraryResourceProvider(), id);
        
        for (Library l : libraryLoader.getLibraries()) {
            VersionedIdentifier vid = l.getIdentifier();
            if (vid.getId().equals(fhirLibrary.getName()) && LibraryResourceHelper.compareVersions(fhirLibrary.getVersion(), vid.getVersion()) == 0) {
                library = l;
                break;
            }
        }

        if (library == null) {
            throw new IllegalArgumentException(String
            .format("Could not resolve primary library for Measure/%s.", measure.getId()));
        }

        return library;
    }
}
