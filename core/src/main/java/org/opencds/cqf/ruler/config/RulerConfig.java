package org.opencds.cqf.ruler.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import org.opencds.cqf.external.AppProperties;
import org.opencds.cqf.external.common.FhirServerConfigCommon;
import org.opencds.cqf.external.common.FhirServerConfigDstu2;
import org.opencds.cqf.external.common.FhirServerConfigDstu3;
import org.opencds.cqf.external.common.FhirServerConfigR4;
import org.opencds.cqf.external.common.FhirServerConfigR5;
import org.opencds.cqf.external.common.FhirTesterConfig;
import org.opencds.cqf.external.cr.CrOperationProviderFactory;
import org.opencds.cqf.external.cr.CrOperationProviderLoader;
import org.opencds.cqf.external.cr.StarterCrDstu3Config;
import org.opencds.cqf.external.cr.StarterCrR4Config;
import org.opencds.cqf.ruler.ServerConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ca.uhn.fhir.batch2.jobs.config.Batch2JobsConfig;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.batch2.JpaBatch2Config;
import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.IEmailSender;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;
import ca.uhn.fhir.jpa.util.LoggingEmailSender;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;

@Configuration
@Import({
		AppProperties.class,
		FhirServerConfigCommon.class,
		FhirServerConfigDstu2.class,
		FhirServerConfigDstu3.class,
		FhirServerConfigR4.class,
		FhirServerConfigR5.class,
		JpaBatch2Config.class,
		Batch2JobsConfig.class,
		FhirTesterConfig.class,
})
public class RulerConfig {
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

	@Primary
	@Bean
	IEmailSender emailSender() {
		return new LoggingEmailSender();
	}
}
