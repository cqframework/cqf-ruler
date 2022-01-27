package org.opencds.cqf.ruler.qualitymeasure;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.cpg.CpgConfig;
import org.opencds.cqf.ruler.cql.CqlConfig;
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
@Import({CqlConfig.class, CpgConfig.class})
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
	public CommonDataRequirementsUtility commonDataRequirementsUtil() {
		return new CommonDataRequirementsUtility();
	}

}
