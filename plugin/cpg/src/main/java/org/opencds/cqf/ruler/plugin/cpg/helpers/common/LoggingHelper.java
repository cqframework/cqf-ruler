package org.opencds.cqf.ruler.plugin.cpg.helpers.common;

import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.ruler.plugin.cpg.CpgProperties;
import org.springframework.beans.factory.annotation.Autowired;

public class LoggingHelper {

	@Autowired
	private CpgProperties cpgProperties;

    public DebugMap getDebugMap() {
        DebugMap debugMap = new DebugMap();
        if (cpgProperties.getLogEnabled()) {
            debugMap.setIsLoggingEnabled(true);
        }
        return debugMap;
    }
}
