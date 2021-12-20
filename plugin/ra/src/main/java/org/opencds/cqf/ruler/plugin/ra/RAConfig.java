package org.opencds.cqf.ruler.plugin.ra;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.ra", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RAConfig {

	@Bean
	public RAProperties RAProperties() {
		return new RAProperties();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4ReportProvider() {
		return new org.opencds.cqf.ruler.plugin.ra.r4.ReportProvider();
	}
}
