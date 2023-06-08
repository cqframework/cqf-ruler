package org.opencds.cqf.ruler.config;

import ca.uhn.fhir.jpa.subscription.channel.subscription.SubscriptionChannelRegistry;
import ca.uhn.fhir.jpa.subscription.channel.subscription.SubscriptionDeliveryHandlerFactory;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.EmailSenderImpl;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.IEmailSender;
import ca.uhn.fhir.jpa.subscription.match.registry.SubscriptionRegistry;
import ca.uhn.fhir.rest.server.mail.IMailSvc;
import ca.uhn.fhir.rest.server.mail.MailConfig;
import ca.uhn.fhir.rest.server.mail.MailSvc;
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
import org.springframework.context.annotation.Primary;

@Import({
		SubscriptionSubmitterConfig.class,
		SubscriptionProcessorConfig.class,
		SubscriptionChannelConfig.class,
		AppProperties.class,
		JpaBatch2Config.class,
		Batch2JobsConfig.class,
		FhirServerConfigCommon.class
	 })
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

	/*@Bean
	public SubscriptionChannelRegistry subscriptionChannelRegistry() {
		return new SubscriptionChannelRegistry();
	}

	@Bean
	public SubscriptionDeliveryHandlerFactory subscriptionDeliveryHandlerFactory(ApplicationContext theApplicationContext, IEmailSender theEmailSender) {
		return new SubscriptionDeliveryHandlerFactory(theApplicationContext, theEmailSender);
	}
	@Bean
	public SubscriptionRegistry subscriptionRegistry() {
		return new SubscriptionRegistry();
	}

	@Primary
	@Bean
	public IEmailSender emailSender(AppProperties appProperties) {
			MailConfig mailConfig = new MailConfig();

			AppProperties.Subscription.Email email = appProperties.getSubscription().getEmail();
			mailConfig.setSmtpHostname(email.getHost());
			mailConfig.setSmtpPort(email.getPort());
			mailConfig.setSmtpUsername(email.getUsername());
			mailConfig.setSmtpPassword(email.getPassword());
			mailConfig.setSmtpUseStartTLS(email.getStartTlsEnable());

			IMailSvc mailSvc = new MailSvc(mailConfig);
			IEmailSender emailSender = new EmailSenderImpl(mailSvc);

			return emailSender;
	}

	 */
}
