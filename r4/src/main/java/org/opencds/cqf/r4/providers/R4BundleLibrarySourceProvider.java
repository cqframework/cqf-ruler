package org.opencds.cqf.r4.providers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;

import ca.uhn.fhir.util.BundleUtil;

public class R4BundleLibrarySourceProvider extends VersionComparingLibrarySourceProvider  {

    Bundle bundle;
    public R4BundleLibrarySourceProvider(Bundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {
        Objects.requireNonNull(versionedIdentifier, "versionedIdentifier can not be null.");
        
        Library library = this.getLibrary(versionedIdentifier.getId(), versionedIdentifier.getVersion());
        if (library == null ){
            return null;
        }

        return this.getCqlStream(library);
    }

    public Library getLibrary(String name, String version) {
        FhirModelResolver resolver = new R4FhirModelResolver();
        Optional<Library> result = BundleUtil.toListOfResourcesOfType(resolver.getFhirContext(), this.bundle, Library.class)
            .stream()
            .filter(library -> library.getName().equals(name) && library.getVersion().equals(version))
            .findFirst();

        return result.isPresent() ? result.get() : null;
    }

    private InputStream getCqlStream(Library library) {
        if (library.hasContent()) {
            for (Attachment content : library.getContent()) {
                // TODO: Could use this for any content type, would require a mapping from content type to LanguageServer LanguageId
                if (content.getContentType().equals("text/cql")) {
                    return new ByteArrayInputStream(content.getData());
                }
                // TODO: Decompile ELM if no CQL is available?
            }
        }

        return null;
    }
}