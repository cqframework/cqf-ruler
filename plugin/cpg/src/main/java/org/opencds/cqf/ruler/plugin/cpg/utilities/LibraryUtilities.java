package org.opencds.cqf.ruler.plugin.cpg.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;
import org.cqframework.cql.elm.execution.Library.Usings;
import org.cqframework.cql.elm.execution.UsingDef;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.JAXBException;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.tracking.TrackBack;
import org.opencds.cqf.cql.engine.execution.CqlLibraryReader;

public interface LibraryUtilities {
    
    public final Map<String, String> urlsByModelName = new HashMap<String, String>() {
        {
            put("FHIR", "http://hl7.org/fhir");
            put("QDM", "urn:healthit-gov:qdm:v5_4");
        }
    };

    // Returns a list of (Model, Version, Url) for the usings in library. The
    // "System" using is excluded.
    public default List<Triple<String, String, String>> getUsingUrlAndVersion(Usings usings) {
        if (usings == null || usings.getDef() == null) {
            return Collections.emptyList();
        }

        List<Triple<String, String, String>> usingDefs = new ArrayList<>();
        for (UsingDef def : usings.getDef()) {

            if (def.getLocalIdentifier().equals("System"))
                continue;

            usingDefs.add(Triple.of(def.getLocalIdentifier(), def.getVersion(),
                    urlsByModelName.get(def.getLocalIdentifier())));
        }

        return usingDefs;
    }

    public default Library readLibrary(InputStream xmlStream) {
        try {
            return CqlLibraryReader.read(xmlStream);
        } catch (IOException | JAXBException e) {
            throw new IllegalArgumentException("Error encountered while reading ELM xml: " + e.getMessage());
        }
    }

    public default String errorsToString(Iterable<CqlTranslatorException> exceptions) {
        ArrayList<String> errors = new ArrayList<>();
        for (CqlTranslatorException error : exceptions) {
            TrackBack tb = error.getLocator();
            String lines = tb == null ? "[n/a]"
                    : String.format("%s[%d:%d, %d:%d]",
                            (tb.getLibrary() != null ? tb.getLibrary().getId()
                                    + (tb.getLibrary().getVersion() != null ? ("-" + tb.getLibrary().getVersion()) : "")
                                    : ""),
                            tb.getStartLine(), tb.getStartChar(), tb.getEndLine(), tb.getEndChar());
            errors.add(lines + error.getMessage());
        }

        return errors.toString();
    }

    public default CqlTranslator getTranslator(String cql, LibraryManager libraryManager, ModelManager modelManager) {
        return getTranslator(new ByteArrayInputStream(cql.getBytes(StandardCharsets.UTF_8)), libraryManager,
                modelManager);
    }

    public default List<CqlTranslator.Options> generateDefaultTranslatorOptions() {
        ArrayList<CqlTranslator.Options> options = new ArrayList<>();
        options.add(CqlTranslator.Options.EnableAnnotations);
        options.add(CqlTranslator.Options.EnableLocators);
        options.add(CqlTranslator.Options.DisableListDemotion);
        options.add(CqlTranslator.Options.DisableListPromotion);
        options.add(CqlTranslator.Options.DisableMethodInvocation);
        return options;
    }

    public default CqlTranslator getTranslator(InputStream cqlStream, LibraryManager libraryManager,
            ModelManager modelManager) {

        CqlTranslator translator;
        try {
            List<CqlTranslator.Options> options = generateDefaultTranslatorOptions();
            translator = CqlTranslator.fromStream(cqlStream, modelManager, libraryManager,
                    options.toArray(new CqlTranslator.Options[options.size()]));
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    String.format("Errors occurred translating library: %s", e.getMessage()));
        }

        return translator;
    }

    public default Library translateLibrary(String cql, LibraryManager libraryManager, ModelManager modelManager) {
        return translateLibrary(new ByteArrayInputStream(cql.getBytes(StandardCharsets.UTF_8)), libraryManager,
                modelManager);
    }

    public default Library translateLibrary(InputStream cqlStream, LibraryManager libraryManager,
            ModelManager modelManager) {
        CqlTranslator translator = getTranslator(cqlStream, libraryManager, modelManager);
        return readLibrary(new ByteArrayInputStream(translator.toXml().getBytes(StandardCharsets.UTF_8)));
    }

    public default Library translateLibrary(CqlTranslator translator) {
        return readLibrary(new ByteArrayInputStream(translator.toXml().getBytes(StandardCharsets.UTF_8)));
    }

    public default CqlTranslatorOptions getTranslatorOptions() {
        CqlTranslatorOptions cqlTranslatorOptions = new CqlTranslatorOptions();
        cqlTranslatorOptions.getOptions().addAll(generateDefaultTranslatorOptions());
        cqlTranslatorOptions.setAnalyzeDataRequirements(true);
        cqlTranslatorOptions.setCollapseDataRequirements(true);
        return cqlTranslatorOptions;
    }
    
}
