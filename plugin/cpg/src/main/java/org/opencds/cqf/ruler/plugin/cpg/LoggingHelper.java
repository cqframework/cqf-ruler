package org.opencds.cqf.ruler.plugin.cpg;

import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.jpa.starter.AppProperties;

public class LoggingHelper {

    @Autowired
    private static AppProperties applicationProperties;

    public static DebugMap getDebugMap() {
        DebugMap debugMap = new DebugMap();
        if (applicationProperties.getCql_enabled()) {
            debugMap.setIsLoggingEnabled(true);
        }
        return debugMap;
    }
}
