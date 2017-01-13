package org.opencds.cqf.providers;

import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.data.fhir.JpaFhirDataProvider;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.helpers.LibraryHelper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Christopher on 1/11/2017.
 */
public class MeasureLibraryLoader implements LibraryLoader {

    private JpaFhirDataProvider provider;
    private LibraryHelper helper;
    private Map<String, Library> libraries = new HashMap<>();

    public MeasureLibraryLoader(JpaFhirDataProvider provider, LibraryHelper helper) {
        this.provider = provider;
        this.helper = helper;
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
        org.hl7.elm.r1.VersionedIdentifier identifier = new org.hl7.elm.r1.VersionedIdentifier()
                .withId(libraryIdentifier.getId())
                .withSystem(libraryIdentifier.getSystem())
                .withVersion(libraryIdentifier.getVersion());

        MeasureLibrarySourceProvider librarySource = new MeasureLibrarySourceProvider(provider);
        InputStream is = librarySource.getLibrarySource(identifier);

        return helper.resolveLibrary(librarySource.getContentType(), is);
    }

    @Override
    public Library load(VersionedIdentifier versionedIdentifier) {
        return resolveLibrary(versionedIdentifier);
    }
}
