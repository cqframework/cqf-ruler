package org.opencds.cqf.ruler.cr;

import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.cr.r4.provider.CareGapsProvider;
import org.opencds.cqf.ruler.cr.r4.provider.DataOperationsProvider;
import org.opencds.cqf.ruler.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.ruler.external.annotations.OnR4Condition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.cr", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CrConfig {
	@Bean
	public CrProperties crProperties() {
		return new CrProperties();
	}

	@Bean
	SearchParameterResolver searchParameterResolver(FhirContext fhirContext) {
		return new SearchParameterResolver(fhirContext);
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public org.opencds.cqf.ruler.cr.dstu3.provider.ActivityDefinitionApplyProvider dstu3ActivityDefinitionApplyProvider() {
		return new org.opencds.cqf.ruler.cr.dstu3.provider.ActivityDefinitionApplyProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public org.opencds.cqf.ruler.cr.r4.provider.ActivityDefinitionApplyProvider r4ActivityDefinitionApplyProvider() {
		return new org.opencds.cqf.ruler.cr.r4.provider.ActivityDefinitionApplyProvider();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public org.opencds.cqf.ruler.cr.dstu3.ExpressionEvaluation dstu3ExpressionEvaluation() {
		return new org.opencds.cqf.ruler.cr.dstu3.ExpressionEvaluation();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public org.opencds.cqf.ruler.cr.r4.ExpressionEvaluation r4ExpressionEvaluation() {
		return new org.opencds.cqf.ruler.cr.r4.ExpressionEvaluation();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public org.opencds.cqf.ruler.cr.dstu3.provider.PlanDefinitionApplyProvider dstu3PlanDefinitionApplyProvider() {
		return new org.opencds.cqf.ruler.cr.dstu3.provider.PlanDefinitionApplyProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public org.opencds.cqf.ruler.cr.r4.provider.PlanDefinitionApplyProvider r4PlanDefinitionApplyProvider() {
		return new org.opencds.cqf.ruler.cr.r4.provider.PlanDefinitionApplyProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public org.opencds.cqf.ruler.cr.r4.provider.MeasureEvaluateProvider r4MeasureEvaluateProvider() {
		return new org.opencds.cqf.ruler.cr.r4.provider.MeasureEvaluateProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public org.opencds.cqf.ruler.cr.r4.provider.SubmitDataProvider r4SubmitDataProvider() {
		return new org.opencds.cqf.ruler.cr.r4.provider.SubmitDataProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4DataRequirementsProvider() {
		return new DataOperationsProvider();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public OperationProvider dstu3DataRequirementsProvider() {
		return new org.opencds.cqf.ruler.cr.dstu3.provider.DataOperationsProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public CareGapsProvider r4CareGapsProvider() {
		return new org.opencds.cqf.ruler.cr.r4.provider.CareGapsProvider();
	}
}
