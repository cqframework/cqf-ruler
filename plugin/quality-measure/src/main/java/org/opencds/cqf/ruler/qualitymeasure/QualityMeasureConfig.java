package org.opencds.cqf.ruler.qualitymeasure;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.opencds.cqf.ruler.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.ruler.qualitymeasure.common.CommonDataRequirementsUtility;
import org.opencds.cqf.ruler.qualitymeasure.r4.DataOperationsProvider;
import org.opencds.cqf.ruler.external.annotations.OnR4Condition;
import org.opencds.cqf.ruler.qualitymeasure.r4.DataRequirementsUtility;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.qualitymeasure", name ="enabled", havingValue = "true", matchIfMissing=true)
@Import({CqlConfig.class})
public class QualityMeasureConfig {
	@Bean
	public QualityMeasureProperties qualityMeasureProperties() {
		return new QualityMeasureProperties();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4DataRequirementsProvider() {
		return new DataOperationsProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public DataRequirementsUtility r4DataRequirementsUtil() {
		return new DataRequirementsUtility();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public OperationProvider dstu3DataRequirementsProvider() {
		return new org.opencds.cqf.ruler.qualitymeasure.dstu3.DataOperationsProvider();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public org.opencds.cqf.ruler.qualitymeasure.dstu3.DataRequirementsUtility dstu3DataRequirementsUtil() {
		return new org.opencds.cqf.ruler.qualitymeasure.dstu3.DataRequirementsUtility();
	}

	@Bean
	public CommonDataRequirementsUtility commonDataRequirementsUtil() {
		return new CommonDataRequirementsUtility();
	}

}
