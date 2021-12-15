package org.opencds.cqf.ruler.plugin.cdshooks.providers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;

public class PriorityRetrieveProvider implements RetrieveProvider {

    private List<RetrieveProvider> providers = new ArrayList<>();

    public PriorityRetrieveProvider (RetrieveProvider... providers) {
        if (providers != null) {
            for (RetrieveProvider provider : providers) {
                this.providers.add(provider);
            }
        }
    }

    @Override
    public Iterable<Object> retrieve(String context, String contextPath, Object contextValue, String dataType,
            String templateId, String codePath, Iterable<Code> codes, String valueSet, String datePath,
            String dateLowPath, String dateHighPath, Interval dateRange) {

                Iterable<Object> result = null;
                for (RetrieveProvider provider : providers) {
                    result = provider.retrieve(context, contextPath, contextValue, dataType, templateId, codePath, codes, valueSet, datePath, dateLowPath, dateHighPath, dateRange);
                    if (result != null && result instanceof Collection) {
                        if (!((Collection<?>)result).isEmpty()) {
                            return result;
                        }
                    }

                }

                return result;
    }

}
