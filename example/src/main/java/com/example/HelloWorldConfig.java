package com.example;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.external.annotations.OnR4Condition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HelloWorldConfig {

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
