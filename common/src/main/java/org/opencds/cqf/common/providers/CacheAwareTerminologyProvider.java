package org.opencds.cqf.common.providers;

import java.util.Map;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;

public class CacheAwareTerminologyProvider implements TerminologyProvider {

    private TerminologyProvider innerTerminologyProvider;

    private Map<String, Iterable<Code>> globalCodeCacheByUrl;

    public CacheAwareTerminologyProvider(Map<String, Iterable<Code>> globalCodeCacheByUrl, TerminologyProvider innerTerminologyProvider) {
        this.globalCodeCacheByUrl = globalCodeCacheByUrl;
        this.innerTerminologyProvider = innerTerminologyProvider;
    }

    @Override
    public boolean in(Code code, ValueSetInfo valueSet) {
        Iterable<Code> codes = this.expand(valueSet);
        if (codes == null) {
            return false;
        }

        for (Code c : codes) {
            if (c.equivalent(code)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Iterable<Code> expand(ValueSetInfo valueSet) {
        if (this.globalCodeCacheByUrl.containsKey(valueSet.getId())) {
            return this.globalCodeCacheByUrl.get(valueSet.getId());
        }

        Iterable<Code> codes = this.innerTerminologyProvider.expand(valueSet);

        if (codes != null) {
            this.globalCodeCacheByUrl.put(valueSet.getId(), codes);
        }

        return codes;
    }

    @Override
    public Code lookup(Code code, CodeSystemInfo codeSystem) {
        return this.innerTerminologyProvider.lookup(code, codeSystem);
    }
    
}
