package org.opencds.cqf.providers;

import org.hl7.fhir.dstu3.model.Library;

public interface LibraryResourceProvider {
    
    public Library resolveLibraryById(String libraryId);

    public Library resolveLibraryByName(String libraryName, String libraryVersion);


    // Hmmm... Probably need to think through this use case a bit more.
    // Should we throw an exception? Should this be a different interface?
    public void update(org.hl7.fhir.dstu3.model.Library library);


    // This function assums that you're selecting from a set of libraries with the same name.
    // It returns the closest matching version, or the max version if no version is specified.
    static Library selectFromList(Iterable<Library> libraries, String libraryVersion) {
        Library library = null;
        Library maxVersion = null;
        for (org.hl7.fhir.dstu3.model.Library l : libraries) {
            if ((libraryVersion != null && l.getVersion().equals(libraryVersion)) ||
               (libraryVersion == null && !l.hasVersion()))
            {
                library = l;
            }

            if (maxVersion == null || org.opencds.cqf.providers.LibraryResourceProvider.compareVersions(maxVersion.getVersion(), l.getVersion()) < 0){
                maxVersion = l;
            }
        }

        // If we were not given a version, return the highest found
        if (libraryVersion == null && maxVersion != null) {
            return maxVersion;
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