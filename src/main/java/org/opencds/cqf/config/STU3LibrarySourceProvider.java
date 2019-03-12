package org.opencds.cqf.config;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;

import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import ca.uhn.fhir.rest.param.StringParam;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.helpers.LibraryResourceHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
        try {
            org.hl7.fhir.dstu3.model.Library lib = LibraryResourceHelper.resolveLibrary(provider, versionedIdentifier.getId(), versionedIdentifier.getVersion());
            for (org.hl7.fhir.dstu3.model.Attachment content : lib.getContent()) {
                if (content.getContentType().equals("text/cql")) {
                    return new ByteArrayInputStream(content.getData());
                }
            }
        }
        catch(Exception e){}

        return this.innerProvider.getLibrarySource(versionedIdentifier);
    }
}