package org.opencds.cqf.common.helpers;

import org.opencds.cqf.common.config.HapiProperties;
import org.opencds.cqf.cql.engine.debug.DebugMap;

public class LoggingHelper {

    public static DebugMap getDebugMap() {
        DebugMap debugMap = new DebugMap();
        if (HapiProperties.getLogEnabled()) {
            debugMap.setIsLoggingEnabled(true);
        }
        return debugMap;
    }
}
