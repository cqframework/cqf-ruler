package com.converter;

import org.opencds.cqf.external.annotations.OnR4Condition;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConverterConfig {
  @Bean
	public ConverterProperties converterProperties() {
		return new ConverterProperties();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider converterProvider() {
		return new ConverterProvider();
	}
}
