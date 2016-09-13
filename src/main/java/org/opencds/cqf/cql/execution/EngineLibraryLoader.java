package org.opencds.cqf.cql.execution;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.CqlTranslatorIncludeException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Bryn on 9/12/2016.
 */
public class EngineLibraryLoader implements LibraryLoader {

    private Map<String, Library> libraries = new HashMap<>();

    public EngineLibraryLoader(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
    }

    private LibraryManager libraryManager;
    public LibraryManager getLibraryManager() {
        return libraryManager;
    }
    public void setLibraryManager(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
    }

    @Override
    public Library load(VersionedIdentifier versionedIdentifier) {
        Library library = libraries.get(versionedIdentifier.getId());
        if (library == null) {
            org.hl7.elm.r1.VersionedIdentifier elmIdentifier =
                    new org.hl7.elm.r1.VersionedIdentifier()
                            .withId(versionedIdentifier.getId())
                            .withVersion(versionedIdentifier.getVersion());

            List<CqlTranslatorException> errors = new ArrayList<>();
            org.cqframework.cql.cql2elm.model.TranslatedLibrary librarySource = libraryManager.resolveLibrary(elmIdentifier, errors);
            if (errors.size() > 0) {
                throw new CqlTranslatorIncludeException(String.format("Errors occurred translating library %s, version %s.",
                        versionedIdentifier.getId(), versionedIdentifier.getVersion()), versionedIdentifier.getId(), versionedIdentifier.getVersion());
            }

            try {
                library = CqlLibraryReader.read(CqlTranslator.convertToXML(librarySource.getLibrary()));
            }
            catch (IOException e) {
                throw new CqlTranslatorIncludeException(String.format("Errors occurred translating library %s, version %s.",
                        versionedIdentifier.getId(), versionedIdentifier.getVersion()), versionedIdentifier.getId(), versionedIdentifier.getVersion(), e);
            }
            catch (JAXBException e) {
                throw new CqlTranslatorIncludeException(String.format("Errors occurred translating library %s, version %s.",
                        versionedIdentifier.getId(), versionedIdentifier.getVersion()), versionedIdentifier.getId(), versionedIdentifier.getVersion(), e);
            }

            libraries.put(versionedIdentifier.getId(), library);
        }

        return library;
    }
}
