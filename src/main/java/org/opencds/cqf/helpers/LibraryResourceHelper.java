package org.opencds.cqf.helpers;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.StringParam;

import org.hl7.fhir.dstu3.model.IdType;
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

    public static org.hl7.fhir.dstu3.model.Library resolveLibraryById(LibraryResourceProvider provider, String libraryId) {
        try {
            return provider.getDao().read(new IdType(libraryId));
        }
        catch (Exception e) {
            throw new IllegalArgumentException(String.format("Could not resolve library id %s", libraryId));
        }
    }

    public static org.hl7.fhir.dstu3.model.Library resolveLibraryByName(LibraryResourceProvider provider, String libraryName, String libraryVersion) {
        org.hl7.fhir.dstu3.model.Library library = null;
        org.hl7.fhir.dstu3.model.Library maxVersion = null;

        Iterable<org.hl7.fhir.dstu3.model.Library> libraries = getLibrariesByName(provider, libraryName);
        for (org.hl7.fhir.dstu3.model.Library l : libraries) {
            if ((libraryVersion != null && l.getVersion().equals(libraryVersion)) ||
               (libraryVersion == null && !l.hasVersion()))
            {
                library = l;
            }

            if (maxVersion == null || compareVersions(maxVersion.getVersion(), l.getVersion()) < 0){
                maxVersion = l;
            }
        }

        // If we were not given a version, return the highest found
        if (libraryVersion == null && maxVersion != null) {
            return maxVersion;
        }

        if (library == null) {
            throw new IllegalArgumentException(String.format("Could not resolve library name %s", libraryName));
        }

        return library;
    }

    public static int compareVersions(String version1, String version2)
    {
        // Treat null as MAX VERSION
        if (version1 == null && version2 == null) {
            return 0;
        }

        if (version1 != null && version2 == null) {
            return -1;
        }

        if (version1 == null && version2 != null) {
            return 1;
        }

        String[] string1Vals = version1.split("\\.");
        String[] string2Vals = version2.split("\\.");
    
        int length = Math.max(string1Vals.length, string2Vals.length);
    
        for (int i = 0; i < length; i++)
        {
            Integer v1 = (i < string1Vals.length)?Integer.parseInt(string1Vals[i]):0;
            Integer v2 = (i < string2Vals.length)?Integer.parseInt(string2Vals[i]):0;
    
            //Making sure Version1 bigger than version2
            if (v1 > v2)
            {
                return 1;
            }
            //Making sure Version1 smaller than version2
            else if(v1 < v2)
            {
                return -1;
            }
        }
    
        //Both are equal
        return 0;
    }
}
