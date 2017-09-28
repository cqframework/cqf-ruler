package org.opencds.cqf.config;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.IdType;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.helpers.LibraryHelper;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Christopher on 1/11/2017.
 */
public class STU3LibraryLoader implements LibraryLoader {

//    private ModelManager modelManager;
    private LibraryManager libraryManager;
    private LibraryHelper libraryHelper;
    private ModelManager modelManager;
    private LibraryResourceProvider provider;
    private Map<String, Library> libraries = new HashMap<>();

    public Map<String, Library> getLibraries() {
        return this.libraries;
    }

    public STU3LibraryLoader(LibraryResourceProvider provider, LibraryManager libraryManager, ModelManager modelManager) {
//        this.modelManager = modelManager;
        this.libraryManager = libraryManager;
        this.libraryHelper = new LibraryHelper();
        this.modelManager = modelManager;
        this.provider = provider;
    }

    private Library resolveLibrary(VersionedIdentifier libraryIdentifier) {
        if (libraryIdentifier == null) {
            throw new IllegalArgumentException("Library identifier is null.");
        }

        if (libraryIdentifier.getId() == null) {
            throw new IllegalArgumentException("Library identifier id is null.");
        }

        Library library = libraries.get(libraryIdentifier.getId());
        if (library != null && libraryIdentifier.getVersion() != null
                && !libraryIdentifier.getVersion().equals(library.getIdentifier().getVersion())) {
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
        IdType id = new IdType(libraryIdentifier.getId());
        org.hl7.fhir.dstu3.model.Library library = provider.getDao().read(id);

        for (org.hl7.fhir.dstu3.model.Attachment content : library.getContent()) {
            if (content.getContentType().equals("application/elm+xml")) {
                return libraryHelper.readLibrary(new ByteArrayInputStream(content.getData()));
            }
            else if (content.getContentType().equals("text/cql")) {
                return libraryHelper.translateLibrary(new ByteArrayInputStream(content.getData()), libraryManager, modelManager);
            }
        }

        org.hl7.elm.r1.VersionedIdentifier identifier = new org.hl7.elm.r1.VersionedIdentifier()
                .withId(libraryIdentifier.getId())
                .withSystem(libraryIdentifier.getSystem())
                .withVersion(libraryIdentifier.getVersion());

        ArrayList<CqlTranslatorException> errors = new ArrayList<>();
        org.hl7.elm.r1.Library translatedLibrary = libraryManager.resolveLibrary(identifier, errors).getLibrary();

        if (errors.size() > 0) {
            throw new IllegalArgumentException(libraryHelper.errorsToString(errors));
        }

        try {
            return libraryHelper.readLibrary(new ByteArrayInputStream(CqlTranslator.convertToXML(translatedLibrary).getBytes(StandardCharsets.UTF_8)));
        } catch (JAXBException e) {
            throw new IllegalArgumentException(String.format("Errors occurred translating library %s%s.",
                    identifier.getId(), identifier.getVersion() != null ? ("-" + identifier.getVersion()) : ""));
        }
    }

    @Override
    public Library load(VersionedIdentifier versionedIdentifier) {
        return resolveLibrary(versionedIdentifier);
    }
}
