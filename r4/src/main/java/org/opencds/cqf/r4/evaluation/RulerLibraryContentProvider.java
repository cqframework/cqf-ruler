package org.opencds.cqf.r4.evaluation;

import java.util.List;

import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BaseFhirLibraryContentProvider;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;

public class RulerLibraryContentProvider extends BaseFhirLibraryContentProvider {

    private DaoRegistry daoRegistry;

    public RulerLibraryContentProvider(DaoRegistry daoRegistry) {
        super(new org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory());
        this.daoRegistry = daoRegistry;
    }

    @Override
    protected IBaseResource getLibrary(VersionedIdentifier libraryIdentifier) {
        // SearchParameterMap map = SearchParameterMap.newSynchronous();
        // map.add("name", new TokenParam(libraryIdentifier.getId()));

        // if (libraryIdentifier.getVersion() != null) {
        //     map.add("version", new TokenParam(libraryIdentifier.getVersion()));
        // }

        List<IBaseResource> libraries = this.daoRegistry.getResourceDao(Library.class).search(SearchParameterMap.newSynchronous()).getAllResources();

        for (IBaseResource r : libraries) {
            Library l = (Library)r;

            if (l.hasName() && l.getName().equals(libraryIdentifier.getId()) && (libraryIdentifier.getVersion() == null || (l.hasVersion() && l.getVersion().equals(libraryIdentifier.getVersion())))) {
                return l;
            }
        }

        return null;
    }
}
