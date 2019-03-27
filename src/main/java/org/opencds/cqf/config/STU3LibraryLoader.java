package org.opencds.cqf.config;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.IdType;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.helpers.LibraryResourceHelper;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.opencds.cqf.helpers.LibraryHelper.*;

public class STU3LibraryLoader implements LibraryLoader {

    private LibraryManager libraryManager;
    private ModelManager modelManager;
    private LibraryResourceProvider provider;
    private Map<String, Library> libraries = new HashMap<>();

    public Map<String, Library> getLibraries() {
        return this.libraries;
    }
    public void putLibrary(String id, Library library) {
        libraries.put(id, library);
    }

    public STU3LibraryLoader(LibraryResourceProvider provider, LibraryManager libraryManager, ModelManager modelManager) {
        this.libraryManager = libraryManager;
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
        else if (library == null) {
            library = loadLibrary(libraryIdentifier);
            libraries.put(libraryIdentifier.getId(), library);
        }

        return library;
    }

    public Library toElmLibrary(org.hl7.fhir.dstu3.model.Library library) {
        InputStream is = null;
        org.hl7.fhir.dstu3.model.Attachment cqlContent = null;
        org.hl7.fhir.dstu3.model.Attachment elmContent = null;
        for (org.hl7.fhir.dstu3.model.Attachment content : library.getContent()) {
            if (content.hasData()) {
                if (content.getContentType().equals("application/elm+xml")) {
                    elmContent = content;
                } else if (content.getContentType().equals("text/cql")) {
                    cqlContent = content;
                }
            }
        }
        return cqlContent != null ? translateLibrary(new ByteArrayInputStream(cqlContent.getData()), libraryManager, modelManager) :
            elmContent != null ? readLibrary(new ByteArrayInputStream(elmContent.getData())) : null;
    }

    private Library loadLibrary(VersionedIdentifier libraryIdentifier) {
        org.hl7.fhir.dstu3.model.Library library = null;
        try
        {
            library = LibraryResourceHelper.resolveLibrary(provider, libraryIdentifier.getId().replaceAll("_", "-"), libraryIdentifier.getVersion());
        }
        catch (Exception e)
        {
            try {
                IdType id = new IdType(libraryIdentifier.getId().replaceAll("_", "-"));
                library = provider.getDao().read(id);
            }
            catch (Exception ex){ 
            }
        }

        if (library == null) {
            throw new IllegalArgumentException(String.format("Could not resolve library by name or Id %s", libraryIdentifier.getId()));
        }

        Library elmLibrary = toElmLibrary(library);
        if (elmLibrary != null) {
            return elmLibrary;
        }

        org.hl7.elm.r1.VersionedIdentifier identifier = new org.hl7.elm.r1.VersionedIdentifier()
                .withId(libraryIdentifier.getId())
                .withSystem(libraryIdentifier.getSystem())
                .withVersion(libraryIdentifier.getVersion());

        ArrayList<CqlTranslatorException> errors = new ArrayList<>();
        org.hl7.elm.r1.Library translatedLibrary = libraryManager.resolveLibrary(identifier, errors).getLibrary();

        if (errors.size() > 0) {
            throw new IllegalArgumentException(errorsToString(errors));
        }
        try {
            return readLibrary(
                            new ByteArrayInputStream(
                                    getTranslator("", libraryManager, modelManager)
                                            .convertToXml(translatedLibrary).getBytes(StandardCharsets.UTF_8)
                            )
                    );
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
