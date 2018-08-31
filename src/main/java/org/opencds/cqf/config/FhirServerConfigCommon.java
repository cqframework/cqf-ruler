package org.opencds.cqf.config;

import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirServerConfigCommon {

    @Bean
    public IServerInterceptor loggingInterceptor() {
        LoggingInterceptor retVal = new LoggingInterceptor();
        retVal.setLoggerName("cqfruler.access");
        retVal.setMessageFormat(
                "Path[${servletPath}] Source[${requestHeader.x-forwarded-for}] Operation[${operationType} ${operationName} ${idOrResourceName}] UA[${requestHeader.user-agent}] Params[${requestParameters}] ResponseEncoding[${responseEncodingNoDefault}]");
        retVal.setLogExceptions(true);
        retVal.setErrorMessageFormat("ERROR - ${requestVerb} ${requestUrl}");
        return retVal;
    }

    @Bean
    public IServerInterceptor requestLoggingInterceptor() {
        LoggingInterceptor retVal = new LoggingInterceptor();
        retVal.setLoggerName("cqfruler.request");
        retVal.setMessageFormat("${requestVerb} ${servletPath} -\n${requestBodyFhir}");
        retVal.setLogExceptions(false);
        return retVal;
    }
}
