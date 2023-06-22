package org.opencds.cqf.ruler.cr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.opencds.cqf.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.external.annotations.OnR4Condition;
import org.opencds.cqf.external.cr.PostInitProviderRegisterer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.cr.common.ILibraryManagerFactory;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.rulercr", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CrConfig {

	@Bean
	CrRulerProperties crRulerProperties(){return new CrRulerProperties();}

	@Bean
	JpaCRFhirDalFactory jpaCRFhirDalFactory(DaoRegistry daoRegistry) {
		return rd -> new JpaCRFhirDal(daoRegistry, rd);
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

	@Bean
	CrProviderFactory crOperationFactory() {
		return new CrProviderFactory();
	}

	@Bean
	CrProviderLoader crProviderLoader(FhirContext theFhirContext, ResourceProviderFactory theResourceProviderFactory,
												  CrProviderFactory theCrProviderFactory, PostInitProviderRegisterer thePostInitProviderRegisterer) {
		return new CrProviderLoader(theFhirContext, theResourceProviderFactory, theCrProviderFactory, thePostInitProviderRegisterer);
	}

}
