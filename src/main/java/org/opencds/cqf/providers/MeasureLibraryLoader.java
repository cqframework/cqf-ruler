package org.opencds.cqf.providers;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.helpers.LibraryHelper;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Christopher on 1/11/2017.
 */
public class MeasureLibraryLoader implements LibraryLoader {

    private LibraryManager libraryManager;
    private Map<String, Library> libraries = new HashMap<>();

    public MeasureLibraryLoader(LibraryManager libraryManager) {
        if (libraryManager == null) {
            throw new IllegalArgumentException("libraryManager is null");
        }

        this.libraryManager = libraryManager;
    }

    private Library resolveLibrary(VersionedIdentifier libraryIdentifier) {
        if (libraryIdentifier == null) {
            throw new IllegalArgumentException("Library identifier is null.");
        }

        if (libraryIdentifier.getId() == null) {
            throw new IllegalArgumentException("Library identifier id is null.");
        }

        Library library = libraries.get(libraryIdentifier.getId());
        if (library != null && libraryIdentifier.getVersion() != null && !libraryIdentifier.getVersion().equals(library.getIdentifier().getVersion())) {
            throw new IllegalArgumentException(String.format("Could not load library %s, version %s because version %s is already loaded.",
                    libraryIdentifier.getId(), libraryIdentifier.getVersion(), library.getIdentifier().getVersion()));
        }
        else {
            library = loadLibrary(libraryIdentifier);
            libraries.put(libraryIdentifier.getId(), library);
        }

        return library;
    }

    private Library loadLibrary(VersionedIdentifier libraryIdentifier) {
        List<CqlTranslatorException> errors = new ArrayList<>();
        org.hl7.elm.r1.VersionedIdentifier identifier = new org.hl7.elm.r1.VersionedIdentifier()
                .withId(libraryIdentifier.getId())
                .withSystem(libraryIdentifier.getSystem())
                .withVersion(libraryIdentifier.getVersion());

        org.cqframework.cql.cql2elm.model.TranslatedLibrary translatedLibrary = libraryManager.resolveLibrary(identifier, errors);

        Library library;
        try {
            String xml = CqlTranslator.convertToXML(translatedLibrary.getLibrary());
            library = LibraryHelper.translateDefault(xml);
        } catch (JAXBException e) {
            throw new RuntimeException(String.format("Errors encountered while loading library %s: %s", libraryIdentifier.getId(), e.getMessage()));
        }

        return library;
    }

    @Override
    public Library load(VersionedIdentifier versionedIdentifier) {
        return null;
    }
}
