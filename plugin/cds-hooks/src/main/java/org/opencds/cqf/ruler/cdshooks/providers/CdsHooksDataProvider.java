package org.opencds.cqf.ruler.cdshooks.providers;

import org.opencds.cqf.cql.engine.retrieve.TerminologyAwareRetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;

public class CdsHooksDataProvider extends TerminologyAwareRetrieveProvider {
    @Override
    public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType,
                                     String templateId, String codePath, Iterable<Code> codes, String valueSet, String datePath,
                                     String dateLowPath, String dateHighPath, Interval dateRange) {
        return null;
    }
}
