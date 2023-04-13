package org.opencds.cqf.ruler.cr;

import org.opencds.cqf.cql.evaluator.spring.fhir.adapter.AdapterConfiguration;
import org.opencds.cqf.ruler.cr.repo.RulerRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;

@Import(AdapterConfiguration.class)
@Configuration
public class RepositoryConfig {

	@Bean
	IRepositoryFactory repositoryFactory(DaoRegistry theDaoRegistry) {
		return rd -> new RulerRepository(theDaoRegistry, rd, (RestfulServer) rd.getServer());
	}

	@Bean
	CrProviderFactory crProviderFactory() {
		return new CrProviderFactory();
	}

	@Bean
	CrProviderLoader crProviderLoader(FhirContext theFhirContext, ResourceProviderFactory theResourceProviderFactory,
			CrProviderFactory theCrProviderFactory) {
		return new CrProviderLoader(theFhirContext, theResourceProviderFactory, theCrProviderFactory);
	}
}
