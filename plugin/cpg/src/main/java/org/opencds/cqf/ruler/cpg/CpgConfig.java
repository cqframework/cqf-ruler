package org.opencds.cqf.ruler.cpg;

import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.ClientFactory;
import org.opencds.cqf.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.external.annotations.OnR4Condition;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.cr.config.CrDstu3Config;
import ca.uhn.fhir.cr.config.CrR4Config;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.cpg", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({ CrDstu3Config.class, CrR4Config.class })
public class CpgConfig {

	@Bean
	public ca.uhn.fhir.cr.config.CrProperties hapiCrProperties() {
		return new ca.uhn.fhir.cr.config.CrProperties();
	}  

	@Bean
	public CpgProperties cpgProperties() {
		return new CpgProperties();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4LibraryEvaluationProvider() {
		return new org.opencds.cqf.ruler.cpg.r4.provider.LibraryEvaluationProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4CqlExecutionProvider() {
		return new org.opencds.cqf.ruler.cpg.r4.provider.CqlExecutionProvider();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public OperationProvider dstu3CqlExecutionProvider() {
		return new org.opencds.cqf.ruler.cpg.dstu3.provider.CqlExecutionProvider();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public OperationProvider dstu3LibraryEvaluationProvider() {
		return new org.opencds.cqf.ruler.cpg.dstu3.provider.LibraryEvaluationProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public FhirRestLibrarySourceProviderFactory r4FhirRestLibraryContentProviderFactory() {
		org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory r4AdapterFactory = new org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory();
		return new FhirRestLibrarySourceProviderFactory(new ClientFactory(FhirContext.forR4Cached()), r4AdapterFactory,
				new LibraryVersionSelector(r4AdapterFactory));
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public FhirRestLibrarySourceProviderFactory dstu3FhirRestLibraryContentProviderFactory() {
		org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.AdapterFactory r4AdapterFactory = new org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.AdapterFactory();
		return new FhirRestLibrarySourceProviderFactory(new ClientFactory(FhirContext.forDstu3Cached()),
				r4AdapterFactory, new LibraryVersionSelector(r4AdapterFactory));
	}
	@Bean
	CpgProviderFactory cpgOperationFactory() {
		return new CpgProviderFactory();
	}

	@Bean
	CpgProviderLoader cpgProviderLoader(FhirContext theFhirContext, ResourceProviderFactory theResourceProviderFactory,
												 CpgProviderFactory theCpgProviderFactory) {
		return new CpgProviderLoader(theFhirContext, theResourceProviderFactory, theCpgProviderFactory);
	}
}
