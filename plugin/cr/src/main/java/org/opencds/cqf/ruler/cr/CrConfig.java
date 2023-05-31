package org.opencds.cqf.ruler.cr;

import java.util.function.Function;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.evaluator.measure.MeasureEvaluationOptions;
import org.opencds.cqf.jpa.starter.annotations.OnDSTU3Condition;
import org.opencds.cqf.jpa.starter.annotations.OnR4Condition;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.server.RequestDetails;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.cr", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(CqlConfig.class)
public class CrConfig {
	@Bean
	public CrProperties crProperties() {
		return new CrProperties();
	}

	@Bean
	public MeasureEvaluationOptions measureEvaluationOptions() {
		return crProperties().getMeasureEvaluation();
	}

	@Bean
	SearchParameterResolver searchParameterResolver(FhirContext fhirContext) {
		return new SearchParameterResolver(fhirContext);
	}

	@Bean
	JpaCRFhirDalFactory jpaCRFhirDalFactory(DaoRegistry daoRegistry) {
		return rd -> new JpaCRFhirDal(daoRegistry, rd);
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
	@Conditional(OnDSTU3Condition.class)
	public org.opencds.cqf.ruler.cr.dstu3.provider.MeasureEvaluateProvider dstu3MeasureEvaluateProvider() {
		return new org.opencds.cqf.ruler.cr.dstu3.provider.MeasureEvaluateProvider();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public Function<RequestDetails, org.opencds.cqf.ruler.cr.dstu3.service.MeasureService> dstu3MeasureServiceFactory() {
		return r -> {
			var ms = dstu3measureService();
			ms.setRequestDetails(r);
			return ms;
		};
	}

	@Bean
	@Scope("prototype")
	@Conditional(OnDSTU3Condition.class)
	public org.opencds.cqf.ruler.cr.dstu3.service.MeasureService dstu3measureService() {
		return new org.opencds.cqf.ruler.cr.dstu3.service.MeasureService();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public org.opencds.cqf.ruler.cr.r4.provider.MeasureEvaluateProvider r4MeasureEvaluateProvider() {
		return new org.opencds.cqf.ruler.cr.r4.provider.MeasureEvaluateProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public Function<RequestDetails, org.opencds.cqf.ruler.cr.r4.service.MeasureService> r4MeasureServiceFactory() {
		return r -> {
			var ms = r4measureService();
			ms.setRequestDetails(r);
			return ms;
		};
	}

	@Bean
	@Scope("prototype")
	@Conditional(OnR4Condition.class)
	public org.opencds.cqf.ruler.cr.r4.service.MeasureService r4measureService() {
		return new org.opencds.cqf.ruler.cr.r4.service.MeasureService();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public org.opencds.cqf.ruler.cr.dstu3.provider.SubmitDataProvider dstu3SubmitDataProvider() {
		return new org.opencds.cqf.ruler.cr.dstu3.provider.SubmitDataProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public org.opencds.cqf.ruler.cr.r4.provider.SubmitDataProvider r4SubmitDataProvider() {
		return new org.opencds.cqf.ruler.cr.r4.provider.SubmitDataProvider();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public org.opencds.cqf.ruler.cr.dstu3.provider.CollectDataProvider dstu3CollectDataProvider() {
		return new org.opencds.cqf.ruler.cr.dstu3.provider.CollectDataProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public org.opencds.cqf.ruler.cr.r4.provider.CollectDataProvider r4CollectDataProvider() {
		return new org.opencds.cqf.ruler.cr.r4.provider.CollectDataProvider();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public org.opencds.cqf.ruler.cr.dstu3.provider.DataOperationsProvider dstu3DataRequirementsProvider() {
		return new org.opencds.cqf.ruler.cr.dstu3.provider.DataOperationsProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public org.opencds.cqf.ruler.cr.r4.provider.DataOperationsProvider r4DataRequirementsProvider() {
		return new org.opencds.cqf.ruler.cr.r4.provider.DataOperationsProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public org.opencds.cqf.ruler.cr.r4.provider.CareGapsProvider r4CareGapsProvider() {
		return new org.opencds.cqf.ruler.cr.r4.provider.CareGapsProvider();
	}
}
