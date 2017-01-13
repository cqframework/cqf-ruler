package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.IdType;
import org.opencds.cqf.cql.data.fhir.JpaFhirDataProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Created by Christopher on 1/12/2017.
 */
public class MeasureLibrarySourceProvider implements LibrarySourceProvider {

    private JpaFhirDataProvider provider;
    private String contentType;

    public MeasureLibrarySourceProvider(JpaFhirDataProvider provider) {
        this.provider = provider;
    }

    public String getContentType() {
        return this.contentType;
    }

    @Override
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {
        IdType id = new IdType(versionedIdentifier.getId());
        org.hl7.fhir.dstu3.model.Library lib = ((LibraryResourceProvider) provider.resolveResourceProvider("Library"))
                .getDao().read(id);
        contentType = lib.getContentFirstRep().getContentType();
        byte[] data = lib.getContentFirstRep().getData();

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Library does not contain any data");
        }

        return new ByteArrayInputStream(data);
    }
}
