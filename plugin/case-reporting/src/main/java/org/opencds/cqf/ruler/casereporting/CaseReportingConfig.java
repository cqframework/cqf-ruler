package org.opencds.cqf.ruler.casereporting;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import org.opencds.cqf.external.annotations.OnR4Condition;
import org.opencds.cqf.external.cr.PostInitProviderRegisterer;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.casereporting.r4.KnowledgeArtifactProcessor;
import org.opencds.cqf.ruler.casereporting.r4.MeasureDataProcessProvider;
import org.opencds.cqf.ruler.casereporting.r4.ProcessMessageProvider;
import org.opencds.cqf.ruler.casereporting.r4.RepositoryService;
import org.opencds.cqf.ruler.casereporting.r4.TerminologyServerClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.casereporting", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CaseReportingConfig {
	@Bean
	public CaseReportingProperties caseReportingProperties() {
		return new CaseReportingProperties();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4MeasureDataProcessor() {
		return new MeasureDataProcessProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public ProcessMessageProvider r4ProcessMessageProvider() {
		return new ProcessMessageProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4RepositoryServiceProvider() {
		return new RepositoryService();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public KnowledgeArtifactProcessor r4KnowledgeArtifactProcessorProvider() {
		return new KnowledgeArtifactProcessor();
	}
	@Bean
	@Conditional(OnR4Condition.class)
	public TerminologyServerClient r4TerminologyClientProvider() {
		return new TerminologyServerClient();
	}

	@Bean
	CaseReportingProviderFactory cpgOperationFactory() {
		return new CaseReportingProviderFactory();
	}

	@Bean
	CaseReportingProviderLoader cpgProviderLoader(FhirContext theFhirContext, ResourceProviderFactory theResourceProviderFactory,
													CaseReportingProviderFactory theCaseReportingProviderFactory, PostInitProviderRegisterer thePostInitProviderRegisterer) {
		return new CaseReportingProviderLoader(theFhirContext, theResourceProviderFactory, theCaseReportingProviderFactory,
			thePostInitProviderRegisterer);
	}
}
