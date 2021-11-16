package org.opencds.cqf.ruler.plugin.hello;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class HelloWorldConfig {
    
    @Lazy
    @Bean
    public HelloWorldProperties helloWorldProperties() {
        return new HelloWorldProperties();
    }

    @Bean
    @Conditional(OnR4Condition.class)
    public OperationProvider helloWorldProvider() {
        return new HelloWorldProvider();
    }
}
