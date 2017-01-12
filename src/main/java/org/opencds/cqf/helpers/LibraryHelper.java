package org.opencds.cqf.helpers;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.tracking.TrackBack;
import org.opencds.cqf.cql.execution.CqlLibraryReader;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Created by Christopher on 1/11/2017.
 */
public class LibraryHelper {

    private LibraryManager libraryManager;
    private ModelManager modelManager;

    public LibraryHelper(LibraryManager libraryManager, ModelManager modelManager) {
        this.modelManager = modelManager;
        this.libraryManager = libraryManager;
    }

    public Library resolveLibrary(String contentType, byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Library does not contain any data");
        }
        // No need to check contentType as it is required if data is present
        // http://build.fhir.org/datatypes-definitions.html#attachment
        InputStream is = new ByteArrayInputStream(data);
        switch (contentType) {
            case "text/cql":
                return translate(new String(data));
            case "application/fhir+xml":
                return readLibrary(is);
            default:
                throw new IllegalArgumentException("Invalid library content type: " + contentType);
        }
    }

    public Library readLibrary(InputStream xmlStream) {
        try {
            return CqlLibraryReader.read(xmlStream);
        } catch (IOException | JAXBException e) {
            throw new IllegalArgumentException("Error encountered while reading ELM xml: " + e.getMessage());
        }
    }

    public Library translate(String cql) {
        ArrayList<CqlTranslator.Options> options = new ArrayList<>();
        options.add(CqlTranslator.Options.EnableDateRangeOptimization);
        CqlTranslator translator = CqlTranslator.fromText(cql, modelManager, libraryManager, options.toArray(new CqlTranslator.Options[options.size()]));

        if (translator.getErrors().size() > 0) {
            ArrayList<String> errors = new ArrayList<>();
            for (CqlTranslatorException error : translator.getErrors()) {
                TrackBack tb = error.getLocator();
                String lines = tb == null ? "[n/a]" : String.format("[%d:%d, %d:%d]",
                        tb.getStartLine(), tb.getStartChar(), tb.getEndLine(), tb.getEndChar());
                errors.add(lines + error.getMessage());
            }
            throw new IllegalArgumentException(errors.toString());
        }

        return readLibrary(new ByteArrayInputStream(translator.toXml().getBytes(StandardCharsets.UTF_8)));
    }

    public static Library translateDefault(String cql) {
        ModelManager mm = new ModelManager();
        LibraryHelper helper = new LibraryHelper(new LibraryManager(mm), mm);
        return helper.translate(cql);
    }
}
