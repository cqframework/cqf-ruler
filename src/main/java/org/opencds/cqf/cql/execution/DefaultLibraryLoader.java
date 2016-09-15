package org.opencds.cqf.cql.execution;

import org.apache.commons.lang3.NotImplementedException;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;

/**
 * Created by Bryn on 9/12/2016.
 */
public class DefaultLibraryLoader implements LibraryLoader {
    @Override
    public Library load(VersionedIdentifier libraryIdentifier) {
        throw new NotImplementedException("Library loader is not implemented.");
    }
}
