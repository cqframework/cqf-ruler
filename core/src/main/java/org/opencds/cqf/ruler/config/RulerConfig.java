package org.opencds.cqf.ruler.config;

import org.opencds.cqf.external.AppProperties;
import org.opencds.cqf.ruler.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.EmailSenderImpl;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.IEmailSender;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.mail.IMailSvc;
import ca.uhn.fhir.rest.server.mail.MailConfig;
import ca.uhn.fhir.rest.server.mail.MailSvc;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;

@Configuration
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

	@Bean
	IEmailSender emailSender(AppProperties appProperties) {
		MailConfig mailConfig = new MailConfig();
		AppProperties.Subscription.Email email = appProperties.getSubscription().getEmail();
		mailConfig.setSmtpHostname(email.getHost());
		mailConfig.setSmtpPort(email.getPort());
		mailConfig.setSmtpUsername(email.getUsername());
		mailConfig.setSmtpPassword(email.getPassword());
		mailConfig.setSmtpUseStartTLS(email.getStartTlsEnable());

		IMailSvc mailSvc = new MailSvc(mailConfig);
		return new EmailSenderImpl(mailSvc);
	}
}
