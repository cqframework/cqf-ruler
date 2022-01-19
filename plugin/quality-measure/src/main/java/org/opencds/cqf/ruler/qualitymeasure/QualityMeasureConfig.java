package org.opencds.cqf.ruler.qualitymeasure;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.qualitymeasure.r4.DataRequirementsProvider;
import org.opencds.cqf.ruler.external.annotations.OnR4Condition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.qualitymeasure", name ="enabled", havingValue = "true", matchIfMissing=true)
public class QualityMeasureConfig {
	@Bean
	public QualityMeasureProperties qualityMeasureProperties() {
		return new QualityMeasureProperties();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4DataRequirementsProvider() {
		return new DataRequirementsProvider();
	}

}
