package org.opencds.cqf.helpers;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.StringParam;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bryn on 3/12/2019.
 */
public class LibraryResourceHelper {
    private static Iterable<org.hl7.fhir.dstu3.model.Library> getLibrariesByName(LibraryResourceProvider provider, String name) {
        // Search for libraries by name
        SearchParameterMap map = new SearchParameterMap();
        map.add("name", new StringParam(name, true));
        ca.uhn.fhir.rest.api.server.IBundleProvider bundleProvider = provider.getDao().search(map);

        if (bundleProvider.size() == 0) {
            return new ArrayList<>();
        }
        List<IBaseResource> resourceList = bundleProvider.getResources(0, bundleProvider.size());
        return resolveLibraries(resourceList);
    }

    private static Iterable<org.hl7.fhir.dstu3.model.Library> resolveLibraries(List< IBaseResource > resourceList) {
        List<org.hl7.fhir.dstu3.model.Library> ret = new ArrayList<>();
        for (IBaseResource res : resourceList) {
            Class clazz = res.getClass();
            ret.add((org.hl7.fhir.dstu3.model.Library)clazz.cast(res));
        }
        return ret;
    }

    public static org.hl7.fhir.dstu3.model.Library resolveLibrary(LibraryResourceProvider provider, String libraryName, String libraryVersion) {
        org.hl7.fhir.dstu3.model.Library library = null;

        // TODO: Version comparison here...
        Iterable<org.hl7.fhir.dstu3.model.Library> libraries = getLibrariesByName(provider, libraryName);
        for (org.hl7.fhir.dstu3.model.Library l : libraries) {
            if (!l.hasVersion()) {
                library = l;
                break;
            }
            if (libraryVersion != null && l.getVersion().equals(libraryVersion)) {
                library = l;
                break;
            }
        }

        if (library == null) {
            throw new IllegalArgumentException(String.format("Could not resolve library name %s", libraryName));
        }

        return library;
    }
}
