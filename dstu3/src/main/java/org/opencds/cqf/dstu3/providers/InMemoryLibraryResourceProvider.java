package org.opencds.cqf.dstu3.providers;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collection;

import org.hl7.fhir.dstu3.model.Library;

public class InMemoryLibraryResourceProvider implements LibraryResourceProvider {

    private Map<String, Library> libraries = new HashMap<>();

    public InMemoryLibraryResourceProvider() {};

    public InMemoryLibraryResourceProvider(Collection<Library> initialLibraries) {

        for (Library library : initialLibraries) {
            this.update(library);
        }
    }

    @Override
    public Library resolveLibraryById(String libraryId) {
        if (this.libraries.containsKey(libraryId)){
            return this.libraries.get(libraryId);
        }

        throw new IllegalArgumentException(String.format("Could not resolve library id %s", libraryId));
    }

    @Override
    public Library resolveLibraryByName(String libraryName, String libraryVersion) {
        List<Library> libraries = this.libraries.values().stream().filter(x -> x.getName().equals(libraryName)).collect(Collectors.toList());
        Library library = LibraryResourceProvider.selectFromList(libraries, libraryVersion);

        if (library == null) {
            throw new IllegalArgumentException(String.format("Could not resolve library name %s", libraryName));
        }

        return library;
    }

    @Override
    public void update(Library library) {
        this.libraries.put(library.getIdElement().getIdPart(), library);
	}

}