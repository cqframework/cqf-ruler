package org.opencds.cqf.dstu3.helpers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.cqframework.cql.elm.tracking.TrackBack;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.Resource;
import org.opencds.cqf.config.MeasureTranslationOptionsLibraryManager;
import org.opencds.cqf.dstu3.config.STU3LibraryLoader;
import org.opencds.cqf.dstu3.config.STU3LibrarySourceProvider;
import org.opencds.cqf.cql.execution.CqlLibraryReader;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.dstu3.providers.LibraryResourceProvider;

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
        LibraryManager libraryManager = new MeasureTranslationOptionsLibraryManager(modelManager);
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
        options.add(CqlTranslator.Options.EnableAnnotations);
        options.add(CqlTranslator.Options.EnableLocators);
        options.add(CqlTranslator.Options.DisableListDemotion);
        options.add(CqlTranslator.Options.DisableListPromotion);
        options.add(CqlTranslator.Options.DisableMethodInvocation);
        CqlTranslator translator;
        try {
            translator = CqlTranslator.fromStream(cqlStream, modelManager, libraryManager,
                    options.toArray(new CqlTranslator.Options[options.size()]));
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Errors occurred translating library: %s", e.getMessage()));
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

    public static List<org.cqframework.cql.elm.execution.Library> loadLibraries(Measure measure, LibraryLoader libraryLoader, LibraryResourceProvider libraryResourceProvider)
    {
        List<org.cqframework.cql.elm.execution.Library> libraries = new ArrayList<org.cqframework.cql.elm.execution.Library>();

        // load libraries
        for (Reference ref : measure.getLibrary()) {
            // if library is contained in measure, load it into server
            if (ref.getReferenceElement().getIdPart().startsWith("#")) {
                for (Resource resource : measure.getContained()) {
                    if (resource instanceof org.hl7.fhir.dstu3.model.Library
                            && resource.getIdElement().getIdPart().equals(ref.getReferenceElement().getIdPart().substring(1)))
                    {
                        libraryResourceProvider.update((org.hl7.fhir.dstu3.model.Library) resource);
                    }
                }
            }

            // We just loaded it into the server so we can access it by Id
            String id = ref.getReferenceElement().getIdPart();
            if (id.startsWith("#")) {
                id = id.substring(1);
            }

            org.hl7.fhir.dstu3.model.Library library = libraryResourceProvider.resolveLibraryById(id);
            libraries.add(
                libraryLoader.load(new VersionedIdentifier().withId(library.getName()).withVersion(library.getVersion()))
            );
        }

        for (RelatedArtifact artifact : measure.getRelatedArtifact()) {
            if (artifact.hasType() && artifact.getType().equals(RelatedArtifactType.DEPENDSON) && artifact.hasResource() && artifact.getResource().hasReference()) {
                org.hl7.fhir.dstu3.model.Library library = libraryResourceProvider.resolveLibraryById(artifact.getResource().getReferenceElement().getIdPart());
                libraries.add(
                    libraryLoader.load(new VersionedIdentifier().withId(library.getName()).withVersion(library.getVersion()))
                );
            }
        }

        if (libraries.isEmpty()) {
            throw new IllegalArgumentException(String
                    .format("Could not load library source for libraries referenced in Measure/%s.", measure.getId()));
        }

        return libraries;
    }

    public static Library resolveLibraryById(String libraryId, LibraryLoader libraryLoader, LibraryResourceProvider libraryResourceProvider)
    {
        // Library library = null;

        org.hl7.fhir.dstu3.model.Library fhirLibrary = libraryResourceProvider.resolveLibraryById(libraryId);
        return libraryLoader.load(new VersionedIdentifier().withId(fhirLibrary.getName()).withVersion(fhirLibrary.getVersion()));

        // for (Library l : libraryLoader.getLibraries()) {
        //     VersionedIdentifier vid = l.getIdentifier();
        //     if (vid.getId().equals(fhirLibrary.getName()) && LibraryResourceHelper.compareVersions(fhirLibrary.getVersion(), vid.getVersion()) == 0) {
        //         library = l;
        //         break;
        //     }
        // }

        // if (library == null) {

        // }

        // return library;
    }

    public static Library resolvePrimaryLibrary(Measure measure, LibraryLoader libraryLoader, LibraryResourceProvider libraryResourceProvider)
    {
        // default is the first library reference
        String id = measure.getLibraryFirstRep().getReferenceElement().getIdPart();

        Library library = resolveLibraryById(id, libraryLoader, libraryResourceProvider);

        if (library == null) {
            throw new IllegalArgumentException(String
                    .format("Could not resolve primary library for Measure/%s.", measure.getIdElement().getIdPart()));
        }

        return library;
    }

    public static Library resolvePrimaryLibrary(PlanDefinition planDefinition, LibraryLoader libraryLoader, LibraryResourceProvider libraryResourceProvider) {
        String id = planDefinition.getLibraryFirstRep().getReferenceElement().getIdPart();

        Library library = resolveLibraryById(id, libraryLoader, libraryResourceProvider);

        if (library == null) {
            throw new IllegalArgumentException(String.format("Could not resolve primary library for PlanDefinition/%s", planDefinition.getIdElement().getIdPart()));
        }

        return library;
    }
}
