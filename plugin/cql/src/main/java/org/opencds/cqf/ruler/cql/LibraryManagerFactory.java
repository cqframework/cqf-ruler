package org.opencds.cqf.ruler.cql;

import java.util.List;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;

public interface LibraryManagerFactory {
	LibraryManager create(List<LibraryContentProvider> libraryContentProviders);
}
