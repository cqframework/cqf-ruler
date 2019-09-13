package org.opencds.cqf.r4.config;

import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.r4.providers.LibraryResourceProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class R4LibrarySourceProvider implements LibrarySourceProvider {

    private LibraryResourceProvider provider;
    private FhirLibrarySourceProvider innerProvider;

    public R4LibrarySourceProvider(LibraryResourceProvider provider) {
        this.provider = provider;
        this.innerProvider = new FhirLibrarySourceProvider();
    }

    @Override
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {
        try {
            org.hl7.fhir.r4.model.Library lib = this.provider.resolveLibraryByName(versionedIdentifier.getId(), versionedIdentifier.getVersion());
            for (org.hl7.fhir.r4.model.Attachment content : lib.getContent()) {
                if (content.getContentType().equals("text/cql")) {
                    return new ByteArrayInputStream(content.getData());
                }
            }
        }
        catch(Exception e) { }

        return this.innerProvider.getLibrarySource(versionedIdentifier);
    }
}