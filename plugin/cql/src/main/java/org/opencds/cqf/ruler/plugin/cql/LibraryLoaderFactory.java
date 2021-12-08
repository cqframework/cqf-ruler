package org.opencds.cqf.ruler.plugin.cql;

import java.util.List;

import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;

public interface LibraryLoaderFactory {
	LibraryLoader create(List<LibraryContentProvider> libraryContentProviders);
}
