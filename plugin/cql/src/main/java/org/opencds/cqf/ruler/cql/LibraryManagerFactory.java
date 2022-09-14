package org.opencds.cqf.ruler.cql;

import java.util.List;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;

public interface LibraryManagerFactory {
	LibraryManager create(List<LibrarySourceProvider> libraryContentProviders);
}
