package org.opencds.cqf.ruler.config;

import java.util.HashSet;

import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.model.entity.StorageSettings;
import org.hl7.fhir.dstu2.model.Subscription;
import org.opencds.cqf.jpa.starter.AppProperties;
import org.opencds.cqf.ruler.provider.PatchedJpaHibernatePropertiesProvider;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.google.common.base.Strings;

import ca.uhn.fhir.jpa.binary.api.IBinaryStorageSvc;
import ca.uhn.fhir.jpa.binstore.DatabaseBlobBinaryStorageSvcImpl;
import ca.uhn.fhir.jpa.config.HibernatePropertiesProvider;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.jpa.model.config.PartitionSettings.CrossPartitionReferenceMode;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.EmailSenderImpl;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.IEmailSender;
import ca.uhn.fhir.rest.server.mail.IMailSvc;
import ca.uhn.fhir.rest.server.mail.MailConfig;
import ca.uhn.fhir.rest.server.mail.MailSvc;

@Configuration
@EnableTransactionManagement
public class FhirServerConfigCommon {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FhirServerConfigCommon.class);

	public FhirServerConfigCommon(AppProperties appProperties) {
		ourLog.info("Server configured to " + (appProperties.getAllow_contains_searches() ? "allow" : "deny")
				+ " contains searches");
		ourLog.info("Server configured to " + (appProperties.getAllow_multiple_delete() ? "allow" : "deny")
				+ " multiple deletes");
		ourLog.info("Server configured to " + (appProperties.getAllow_external_references() ? "allow" : "deny")
				+ " external references");
		ourLog.info("Server configured to " + (appProperties.getDao_scheduling_enabled() ? "enable" : "disable")
				+ " DAO scheduling");
		ourLog.info("Server configured to " + (appProperties.getDelete_expunge_enabled() ? "enable" : "disable")
				+ " delete expunges");
		ourLog.info("Server configured to " + (appProperties.getExpunge_enabled() ? "enable" : "disable") + " expunges");
		ourLog.info("Server configured to " + (appProperties.getAllow_override_default_search_params() ? "allow" : "deny")
				+ " overriding default search params");
		ourLog.info("Server configured to "
				+ (appProperties.getAuto_create_placeholder_reference_targets() ? "allow" : "disable")
				+ " auto-creating placeholder references");

		if (appProperties.getSubscription().getEmail() != null) {
			org.opencds.cqf.jpa.starter.AppProperties.Subscription.Email email = appProperties.getSubscription().getEmail();
			ourLog.info(
					"Server is configured to enable email with host '" + email.getHost() + "' and port " + email.getPort());
			ourLog.info("Server will use '" + email.getFrom() + "' as the from email address");

			if (!Strings.isNullOrEmpty(email.getUsername())) {
				ourLog.info("Server is configured to use username '" + email.getUsername() + "' for email");
			}

			if (!Strings.isNullOrEmpty(email.getPassword())) {
				ourLog.info("Server is configured to use a password for email");
			}
		}

		if (appProperties.getSubscription().getResthook_enabled()) {
			ourLog.info("REST-hook subscriptions enabled");
		}

		if (appProperties.getSubscription().getEmail() != null) {
			ourLog.info("Email subscriptions enabled");
		}

		if (appProperties.getEnable_index_contained_resource() == Boolean.TRUE) {
			ourLog.info("Indexed on contained resource enabled");
		}
	}

	/**
	 * Configure FHIR properties around the JPA server via this bean
	 */
	@Bean
	public JpaStorageSettings jpaStorageSettings(AppProperties appProperties) {
		JpaStorageSettings jpaStorageSettings = new JpaStorageSettings();

		jpaStorageSettings.setIndexMissingFields(appProperties.getEnable_index_missing_fields() ? JpaStorageSettings.IndexEnabledEnum.ENABLED
				: JpaStorageSettings.IndexEnabledEnum.DISABLED);
		jpaStorageSettings.setAutoCreatePlaceholderReferenceTargets(appProperties.getAuto_create_placeholder_reference_targets());
		jpaStorageSettings.setEnforceReferentialIntegrityOnWrite(appProperties.getEnforce_referential_integrity_on_write());
		jpaStorageSettings.setEnforceReferentialIntegrityOnDelete(appProperties.getEnforce_referential_integrity_on_delete());
		jpaStorageSettings.setAllowContainsSearches(appProperties.getAllow_contains_searches());
		jpaStorageSettings.setAllowMultipleDelete(appProperties.getAllow_multiple_delete());
		jpaStorageSettings.setAllowExternalReferences(appProperties.getAllow_external_references());
		jpaStorageSettings.setSchedulingDisabled(!appProperties.getDao_scheduling_enabled());
		jpaStorageSettings.setDeleteExpungeEnabled(appProperties.getDelete_expunge_enabled());
		jpaStorageSettings.setExpungeEnabled(appProperties.getExpunge_enabled());
		if (appProperties.getSubscription() != null && appProperties.getSubscription().getEmail() != null)
			jpaStorageSettings.setEmailFromAddress(appProperties.getSubscription().getEmail().getFrom());

		Integer maxFetchSize = appProperties.getMax_page_size();
		jpaStorageSettings.setFetchSizeDefaultMaximum(maxFetchSize);
		ourLog.info("Server configured to have a maximum fetch size of "
				+ (maxFetchSize == Integer.MAX_VALUE ? "'unlimited'" : maxFetchSize));

		Long reuseCachedSearchResultsMillis = appProperties.getReuse_cached_search_results_millis();
		jpaStorageSettings.setReuseCachedSearchResultsForMillis(reuseCachedSearchResultsMillis);
		ourLog.info("Server configured to cache search results for {} milliseconds", reuseCachedSearchResultsMillis);

		Long retainCachedSearchesMinutes = appProperties.getRetain_cached_searches_mins();
		jpaStorageSettings.setExpireSearchResultsAfterMillis(retainCachedSearchesMinutes * 60 * 1000);

		if (appProperties.getSubscription() != null) {
			// Subscriptions are enabled by channel type
			if (appProperties.getSubscription().getResthook_enabled()) {
				ourLog.info("Enabling REST-hook subscriptions");
				jpaStorageSettings.addSupportedSubscriptionType(org.hl7.fhir.dstu2.model.Subscription.SubscriptionChannelType.RESTHOOK);
			}
			if (appProperties.getSubscription().getEmail() != null) {
				ourLog.info("Enabling email subscriptions");
				jpaStorageSettings.addSupportedSubscriptionType(org.hl7.fhir.dstu2.model.Subscription.SubscriptionChannelType.EMAIL);
			}
			if (appProperties.getSubscription().getWebsocket_enabled()) {
				ourLog.info("Enabling websocket subscriptions");
				jpaStorageSettings.addSupportedSubscriptionType(
						org.hl7.fhir.dstu2.model.Subscription.SubscriptionChannelType.WEBSOCKET);
			}
		}

		jpaStorageSettings.setFilterParameterEnabled(appProperties.getFilter_search_enabled());
		jpaStorageSettings.setAdvancedHSearchIndexing(appProperties.getAdvanced_lucene_indexing());
		jpaStorageSettings.setTreatBaseUrlsAsLocal(new HashSet<>(appProperties.getLocal_base_urls()));

		//Parallel Batch GET execution settings
		jpaStorageSettings.setBundleBatchPoolSize(appProperties.getBundle_batch_pool_size());
		jpaStorageSettings.setBundleBatchPoolSize(appProperties.getBundle_batch_pool_max_size());
		
		storageSettings(appProperties,
			jpaStorageSettings);
		return jpaStorageSettings;
	}

	@Bean
	public YamlPropertySourceLoader yamlPropertySourceLoader() {
		return new YamlPropertySourceLoader();
	}

	@Bean
	public PartitionSettings partitionSettings(AppProperties appProperties) {
		PartitionSettings jpaStorageSettings = new PartitionSettings();

		// Partitioning
		if (appProperties.getPartitioning() != null) {
			jpaStorageSettings.setPartitioningEnabled(true);
			jpaStorageSettings.setIncludePartitionInSearchHashes(
					appProperties.getPartitioning().getPartitioning_include_in_search_hashes());
			if (appProperties.getPartitioning().getAllow_references_across_partitions()) {
				jpaStorageSettings.setAllowReferencesAcrossPartitions(CrossPartitionReferenceMode.ALLOWED_UNQUALIFIED);
			} else {
				jpaStorageSettings.setAllowReferencesAcrossPartitions(CrossPartitionReferenceMode.NOT_ALLOWED);
			}
		}

		return jpaStorageSettings;
	}

	@Primary
	@Bean
	public HibernatePropertiesProvider jpaStarterDialectProvider(
			LocalContainerEntityManagerFactoryBean myEntityManagerFactory) {
		return new PatchedJpaHibernatePropertiesProvider(myEntityManagerFactory);
	}

	public StorageSettings storageSettings(AppProperties appProperties, JpaStorageSettings jpaStorageSettings) {
		jpaStorageSettings.setAllowContainsSearches(appProperties.getAllow_contains_searches());
		jpaStorageSettings.setAllowExternalReferences(appProperties.getAllow_external_references());
		jpaStorageSettings.setDefaultSearchParamsCanBeOverridden(appProperties.getAllow_override_default_search_params());
		if (appProperties.getSubscription() != null && appProperties.getSubscription().getEmail() != null)
			jpaStorageSettings.setEmailFromAddress(appProperties.getSubscription().getEmail().getFrom());

		// You can enable these if you want to support Subscriptions from your server
		if (appProperties.getSubscription() != null && appProperties.getSubscription().getResthook_enabled() != null) {
			jpaStorageSettings.addSupportedSubscriptionType(Subscription.SubscriptionChannelType.RESTHOOK);
		}

		if (appProperties.getSubscription() != null && appProperties.getSubscription().getEmail() != null) {
			jpaStorageSettings.addSupportedSubscriptionType(Subscription.SubscriptionChannelType.EMAIL);
		}

		jpaStorageSettings.setNormalizedQuantitySearchLevel(appProperties.getNormalized_quantity_search_level());

		jpaStorageSettings.setIndexOnContainedResources(appProperties.getEnable_index_contained_resource());
		jpaStorageSettings.setIndexIdentifierOfType(appProperties.getEnable_index_of_type());
		return jpaStorageSettings;
	}

	/**
	 * The following bean configures the database connection. The 'url' property
	 * value of "jdbc:derby:directory:jpaserver_derby_files;create=true" indicates
	 * that the server should save resources in a
	 * directory called "jpaserver_derby_files".
	 * <p>
	 * A URL to a remote database could also be placed here, along with login
	 * credentials and other properties supported by BasicDataSource.
	 */
	/*
	 * @Bean(destroyMethod = "close")
	 * public BasicDataSource dataSource() throws ClassNotFoundException,
	 * NoSuchMethodException, IllegalAccessException, InvocationTargetException,
	 * InstantiationException {
	 * BasicDataSource retVal = new BasicDataSource();
	 * Driver driver = (Driver)
	 * Class.forName(HapiProperties.getDataSourceDriver()).getConstructor().
	 * newInstance();
	 * jpaStorageSettings.setDriver(driver);
	 * jpaStorageSettings.setUrl(HapiProperties.getDataSourceUrl());
	 * jpaStorageSettings.setUsername(HapiProperties.getDataSourceUsername());
	 * jpaStorageSettings.setPassword(HapiProperties.getDataSourcePassword());
	 * jpaStorageSettings.setMaxTotal(HapiProperties.getDataSourceMaxPoolSize());
	 * return retVal;
	 * }
	 */

	@Lazy
	@Bean
	public IBinaryStorageSvc binaryStorageSvc(AppProperties appProperties) {
		DatabaseBlobBinaryStorageSvcImpl binaryStorageSvc = new DatabaseBlobBinaryStorageSvcImpl();

		if (appProperties.getMax_binary_size() != null) {
			binaryStorageSvc.setMaximumBinarySize(appProperties.getMax_binary_size());
		}

		return binaryStorageSvc;
	}

	@Bean
	public IEmailSender emailSender(AppProperties appProperties) {
		if (appProperties.getSubscription() != null && appProperties.getSubscription().getEmail() != null) {
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

		return null;
	}
}
