package org.opencds.cqf.ruler.ra;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.external.annotations.OnR4Condition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

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
		return new org.opencds.cqf.ruler.ra.r4.ReportProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4RiskAssessmentProvider() {
		return new org.opencds.cqf.ruler.ra.r4.RiskAssessmentProvider();
	}
}
