package org.opencds.cqf.config;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;

import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.IdType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Created by Christopher on 1/12/2017.
 */
public class STU3LibrarySourceProvider implements LibrarySourceProvider {

    private LibraryResourceProvider provider;
    private FhirLibrarySourceProvider innerProvider;

    public STU3LibrarySourceProvider(LibraryResourceProvider provider) {
        this.provider = provider;
        this.innerProvider = new FhirLibrarySourceProvider();
    }

    @Override
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {
        IdType id = new IdType(versionedIdentifier.getId());
        org.hl7.fhir.dstu3.model.Library lib = null;
        try {
            lib = provider.getDao().read(id);
        }
        catch (Exception e) {

        }

        if (lib != null) {
            for (org.hl7.fhir.dstu3.model.Attachment content : lib.getContent()) {
                if (content.getContentType().equals("text/cql")) {
                    return new ByteArrayInputStream(content.getData());
                }
            }
        }

        return this.innerProvider.getLibrarySource(versionedIdentifier);
    }
}