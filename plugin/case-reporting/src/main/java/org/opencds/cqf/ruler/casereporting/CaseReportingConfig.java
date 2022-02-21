package org.opencds.cqf.ruler.casereporting;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.casereporting.r4.MeasureDataProcessProvider;
import org.opencds.cqf.ruler.casereporting.r4.ProcessMessageProvider;
import org.opencds.cqf.ruler.external.annotations.OnR4Condition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.casereporting", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CaseReportingConfig {
	@Bean
	public CaseReportingProperties caseReportingProperties() {
		return new CaseReportingProperties();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4MeasureDataProcessor() {
		return new MeasureDataProcessProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public ProcessMessageProvider r4ProcessMessageProvider() {
		return new ProcessMessageProvider();
	}

}
