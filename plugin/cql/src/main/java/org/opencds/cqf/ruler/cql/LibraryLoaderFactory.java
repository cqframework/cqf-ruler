package org.opencds.cqf.ruler.cql;

import java.util.List;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;

public interface LibraryLoaderFactory {
	LibraryLoader create(List<LibrarySourceProvider> libraryContentProviders);
}
