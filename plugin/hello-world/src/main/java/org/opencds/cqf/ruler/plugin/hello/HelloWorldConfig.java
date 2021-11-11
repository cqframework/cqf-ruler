package org.opencds.cqf.ruler.plugin.hello;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
public class HelloWorldConfig {

    @Bean
    public OperationProvider helloWorldProvider() {
        return new HelloWorldProvider();
    }
}
