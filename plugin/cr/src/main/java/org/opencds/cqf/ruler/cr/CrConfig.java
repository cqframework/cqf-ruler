package org.opencds.cqf.ruler.cr;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.cr.config.BaseClinicalReasoningConfig;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.evaluator.CqlOptions;
import org.opencds.cqf.cql.evaluator.measure.MeasureEvaluationOptions;
import org.opencds.cqf.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.external.annotations.OnR4Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.cr.common.ILibraryManagerFactory;
import ca.uhn.fhir.cr.config.CrDstu3Config;
import ca.uhn.fhir.cr.config.CrR4Config;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;

@Configuration
//@ConditionalOnProperty(prefix = "hapi.fhir.cr", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({ CrR4Config.class, CrDstu3Config.class })
public class CrConfig {
	private static final Logger ourLogger = LoggerFactory.getLogger(CrConfig.class);
	@Bean
	public ca.uhn.fhir.cr.config.CrProperties hapiCrProperties() {
		return new ca.uhn.fhir.cr.config.CrProperties();
	}

	@Bean
	public CqlTranslatorOptions cqlTranslatorOptions(FhirContext theFhirContext, ca.uhn.fhir.cr.config.CrProperties.CqlProperties theCqlProperties) {
		CqlTranslatorOptions options = theCqlProperties.getCqlOptions().getCqlTranslatorOptions();

		if (theFhirContext.getVersion().getVersion().isOlderThan(FhirVersionEnum.R4)
			&& (options.getCompatibilityLevel().equals("1.5") || options.getCompatibilityLevel().equals("1.4"))) {
			ourLogger.warn("{} {} {}",
				"This server is configured to use CQL version > 1.4 and FHIR version <= DSTU3.",
				"Most available CQL content for DSTU3 and below is for CQL versions 1.3.",
				"If your CQL content causes translation errors, try setting the CQL compatibility level to 1.3");
		}

		return options;
	}
	@Bean
	public CqlOptions cqlOptions(ca.uhn.fhir.cr.config.CrProperties theCrProperties) {
		return theCrProperties.getCqlProperties().getCqlOptions();
	}
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
	public ILibraryManagerFactory libraryManagerFactory(
			ModelManager modelManager) {
		return (providers) -> {
			LibraryManager libraryManager = new LibraryManager(modelManager);
			for (LibrarySourceProvider provider : providers) {
				libraryManager.getLibrarySourceLoader().registerProvider(provider);
			}
			return libraryManager;
		};
	}
}
