package org.opencds.cqf.ruler.ecr;

import org.opencds.cqf.external.annotations.OnR4Condition;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransformConfig {
	@Bean
	public TransformProperties transformProperties() {
		return new TransformProperties();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider transformProvider() {
		return new TransformProvider();
	}
}
