package org.opencds.cqf.ruler.plugin.dev;

import ca.uhn.fhir.jpa.starter.annotations.OnDSTU3Condition;
import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.dev", name ="enabled", havingValue = "true")
public class DevToolsConfig {

    @Bean
    @Lazy
    public DevToolsProperties devToolsProperties() {
        return new DevToolsProperties();
    }

    @Bean
    @Conditional(OnR4Condition.class)
    public OperationProvider r4CodeSystemUpdateProvider() {
        return new org.opencds.cqf.ruler.plugin.dev.r4.CodeSystemUpdateProvider();
    }

    @Bean
    @Conditional(OnDSTU3Condition.class)
    public OperationProvider dstu3CodeSystemUpdateProvider() {
        return new org.opencds.cqf.ruler.plugin.dev.dstu3.CodeSystemUpdateProvider();
    }
}
