package org.opencds.cqf.ruler.config;
import org.opencds.cqf.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.batch2.jobs.config.Batch2JobsConfig;
import ca.uhn.fhir.jpa.batch2.JpaBatch2Config;
import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;

@Import({
		AppProperties.class,
		StarterJpaConfig.class,
		FhirServerConfigCommon.class,
		FhirServerConfigDstu2.class,
		FhirServerConfigDstu3.class,
		FhirServerConfigR4.class,
		FhirServerConfigR5.class,
		JpaBatch2Config.class,
		Batch2JobsConfig.class,
		SubscriptionSubmitterConfig.class,
		SubscriptionProcessorConfig.class,
		SubscriptionChannelConfig.class })
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
}
