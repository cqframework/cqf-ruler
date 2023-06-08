package org.opencds.cqf.ruler.config;

import org.opencds.cqf.external.AppProperties;
import org.opencds.cqf.external.common.FhirServerConfigCommon;
import org.opencds.cqf.ruler.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.batch2.jobs.config.Batch2JobsConfig;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.batch2.JpaBatch2Config;
import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;

@Import({
		AppProperties.class,
		JpaBatch2Config.class,
		Batch2JobsConfig.class,
		FhirServerConfigCommon.class,
		SubscriptionSubmitterConfig.class,
		SubscriptionProcessorConfig.class,
		SubscriptionChannelConfig.class })
public class RulerConfig {
	private static Logger log = LoggerFactory.getLogger(RulerConfig.class);

	public class DaoConfigCustomizer {
		public DaoConfigCustomizer(JpaStorageSettings theStorageSettings, ServerProperties serverProperties) {
			theStorageSettings.setMaximumIncludesToLoadPerPage(serverProperties.getMaxIncludesPerPage());
		}
	}

	@Bean
	DaoConfigCustomizer configureTheDao(JpaStorageSettings theStorageSettings, ServerProperties serverProperties) {
		return new DaoConfigCustomizer(theStorageSettings, serverProperties);
	}

	@Bean
	ServerConfig serverConfigurator(RestfulServer server, ApplicationContext applicationContext,
											  JpaStorageSettings myJpaStorageSettings, ISearchParamRegistry mySearchParamRegistry,
											  IFhirSystemDao myFhirSystemDao, IValidationSupport myValidationSupport,
											  ServerProperties myServerProperties) {
		return new ServerConfig(server, applicationContext, myJpaStorageSettings, mySearchParamRegistry,
				myFhirSystemDao, myValidationSupport, myServerProperties);
	}

}
