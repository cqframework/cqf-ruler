package org.opencds.cqf.ruler.cr;

import java.util.function.Function;

import ca.uhn.fhir.cr.config.CrDstu3Config;
import ca.uhn.fhir.cr.config.CrR4Config;
import ca.uhn.fhir.cr.r4.measure.CareGapsService;
import ca.uhn.fhir.cr.r4.measure.MeasureService;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.evaluator.measure.MeasureEvaluationOptions;
import org.opencds.cqf.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.external.annotations.OnR4Condition;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.cr", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({CrR4Config.class, CrDstu3Config.class})
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
	public ca.uhn.fhir.cr.dstu3.activitydefinition.ActivityDefinitionOperationsProvider dstu3ActivityDefinitionApplyProvider() {
		return new ca.uhn.fhir.cr.dstu3.activitydefinition.ActivityDefinitionOperationsProvider();
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
	public ca.uhn.fhir.cr.dstu3.measure.MeasureOperationsProvider dstu3MeasureEvaluateProvider() {
		return new ca.uhn.fhir.cr.dstu3.measure.MeasureOperationsProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public ca.uhn.fhir.cr.r4.measure.MeasureOperationsProvider r4MeasureEvaluateProvider() {
		return new ca.uhn.fhir.cr.r4.measure.MeasureOperationsProvider();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public Function<RequestDetails, ca.uhn.fhir.cr.dstu3.measure.MeasureService> dstu3MeasureServiceFactory(ApplicationContext theApplicationContext) {
		return r -> {
			var ms = theApplicationContext.getBean(ca.uhn.fhir.cr.dstu3.measure.MeasureService.class);
			ms.setRequestDetails(r);
			return ms;
		};
	}

	@Bean
	@Scope("prototype")
	@Conditional(OnDSTU3Condition.class)
	public ca.uhn.fhir.cr.dstu3.measure.MeasureService dstu3measureService() {
		return new ca.uhn.fhir.cr.dstu3.measure.MeasureService();
	}


	@Bean
	@Conditional(OnR4Condition.class)
	public Function<RequestDetails, ca.uhn.fhir.cr.r4.measure.MeasureService> r4MeasureServiceFactory(ApplicationContext theApplicationContext) {
		return r -> {
			var ms = theApplicationContext.getBean(MeasureService.class);
			ms.setRequestDetails(r);
			return ms;
		};
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
	public ca.uhn.fhir.cr.r4.measure.CareGapsOperationProvider r4CareGapsProvider(Function<RequestDetails, CareGapsService> theCareGapsServiceFunction) {
		return new ca.uhn.fhir.cr.r4.measure.CareGapsOperationProvider(theCareGapsServiceFunction);
	}

}
