package org.opencds.cqf.ruler.plugin.cpg.helpers.util;

import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;

import java.util.List;

public interface LibraryLoaderFactory {
	LibraryLoader create(List<LibraryContentProvider> libraryContentProviders);
}
