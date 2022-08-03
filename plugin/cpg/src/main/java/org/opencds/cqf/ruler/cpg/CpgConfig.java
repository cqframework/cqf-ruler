package org.opencds.cqf.ruler.cpg;

import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.ClientFactory;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.opencds.cqf.ruler.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.ruler.external.annotations.OnR4Condition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.context.FhirContext;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.cpg", name ="enabled", havingValue = "true", matchIfMissing=true)
@Import({CqlConfig.class})
public class CpgConfig {
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
	public FhirRestLibraryContentProviderFactory r4FhirRestibraryContentProviderFactory() {
		org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory r4AdapterFactory = new org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory();
		return new FhirRestLibraryContentProviderFactory(new ClientFactory(FhirContext.forR4Cached()), r4AdapterFactory, new LibraryVersionSelector(r4AdapterFactory));
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public FhirRestLibraryContentProviderFactory dstu3FhirRestibraryContentProviderFactory() {
		org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.AdapterFactory r4AdapterFactory = new org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.AdapterFactory();
		return new FhirRestLibraryContentProviderFactory(new ClientFactory(FhirContext.forDstu3Cached()), r4AdapterFactory, new LibraryVersionSelector(r4AdapterFactory));
	}
}
