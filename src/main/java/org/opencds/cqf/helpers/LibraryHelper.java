package org.opencds.cqf.helpers;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.IncludeDef;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.cqframework.cql.elm.tracking.TrackBack;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.cql.execution.CqlLibraryReader;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
            libraryLoader.load(new VersionedIdentifier().withVersion(ref.getReferenceElement().getVersionIdPart())
                    .withId(ref.getReferenceElement().getIdPart()));
        }

        if (libraryLoader.getLibraries().isEmpty()) {
            throw new IllegalArgumentException(String
                    .format("Could not load library source for libraries referenced in Measure/%s.", measure.getId()));
        }
    }

    public static Library resolvePrimaryLibrary(Measure measure, STU3LibraryLoader libraryLoader)
    {
        // default is the first library reference
        Library library = libraryLoader.getLibraries().get(measure.getLibraryFirstRep().getReferenceElement().getIdPart());

        // gather all the population criteria expressions
        List<String> criteriaExpressions = new ArrayList<>();
        for (Measure.MeasureGroupComponent grouping : measure.getGroup()) {
            for (Measure.MeasureGroupPopulationComponent population : grouping.getPopulation()) {
                criteriaExpressions.add(population.getCriteria());
            }
        }

        // check each library to see if it includes the expression namespace - return if true
        for (Library candidate : libraryLoader.getLibraries().values()) {
            for (String expression : criteriaExpressions) {
                String namespace = expression.split("\\.")[0];
                if (!namespace.equals(expression)) {
                    for (IncludeDef include : candidate.getIncludes().getDef()) {
                        if (include.getLocalIdentifier().equals(namespace)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        return library;
    }
}
