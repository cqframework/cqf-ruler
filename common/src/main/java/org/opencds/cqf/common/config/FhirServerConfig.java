package org.opencds.cqf.common.config;

import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.apache.commons.dbcp2.BasicDataSource;
import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cds.providers.ProviderConfiguration;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.cache.IResourceChangeEvent;
import ca.uhn.fhir.jpa.cache.IResourceChangeListener;
import ca.uhn.fhir.jpa.cache.IResourceChangeListenerRegistry;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.jpa.model.entity.ModelConfig;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = "org.opencds.cqf.common")
public class FhirServerConfig {

    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FhirServerConfig.class);

    private Boolean allowContainsSearches = HapiProperties.getAllowContainsSearches();
    private Boolean allowMultipleDelete = HapiProperties.getAllowMultipleDelete();
    private Boolean allowExternalReferences = HapiProperties.getAllowExternalReferences();
    private Boolean expungeEnabled = HapiProperties.getExpungeEnabled();
    private Boolean allowPlaceholderReferences = HapiProperties.getAllowPlaceholderReferences();
    private Boolean allowOverrideDefaultSearchParams = HapiProperties.getAllowOverrideDefaultSearchParams();
    private String emailFrom = HapiProperties.getEmailFrom();

    private Boolean enforceReferentialIntegrityOnWrite = HapiProperties.getEnforceReferentialIntegrityOnWrite();
    private Boolean enforceReferentialIntegrityOnDelete = HapiProperties.getEnforceReferentialIntegrityOnDelete();

    public FhirServerConfig() {
        ourLog.info("Server configured to " + (this.allowContainsSearches ? "allow" : "deny") + " contains searches");
        ourLog.info("Server configured to " + (this.allowMultipleDelete ? "allow" : "deny") + " multiple deletes");
        ourLog.info("Server configured to " + (this.allowExternalReferences ? "allow" : "deny") + " external references");
        ourLog.info("Server configured to " + (this.expungeEnabled ? "enable" : "disable") + " expunges");
        ourLog.info("Server configured to " + (this.allowPlaceholderReferences ? "allow" : "deny") + " placeholder references");
        ourLog.info("Server configured to " + (this.allowOverrideDefaultSearchParams ? "allow" : "deny")
                + " overriding default search params");
        ourLog.info("Server configured to " + (this.enforceReferentialIntegrityOnDelete ? "enforce" : "ignore")
                + " referential integrity on delete");
        ourLog.info("Server configured to " + (this.enforceReferentialIntegrityOnDelete ? "enforce" : "ignore")
                + " referential integrity on write");
    }

    /**
     * Configure FHIR properties around the the JPA server via this bean
     */
    @Bean()
    public DaoConfig daoConfig() {
        DaoConfig retVal = new DaoConfig();

        retVal.setAllowContainsSearches(this.allowContainsSearches);
        retVal.setAllowMultipleDelete(this.allowMultipleDelete);
        retVal.setAllowExternalReferences(this.allowExternalReferences);
        retVal.setExpungeEnabled(this.expungeEnabled);
        retVal.setAutoCreatePlaceholderReferenceTargets(this.allowPlaceholderReferences);
        retVal.setEmailFromAddress(this.emailFrom);
        retVal.setEnforceReferentialIntegrityOnDelete(this.enforceReferentialIntegrityOnDelete);
        retVal.setEnforceReferentialIntegrityOnWrite(this.enforceReferentialIntegrityOnWrite);
        retVal.setMaximumExpansionSize(100000);

        retVal.setMaximumExpansionSize(100000);

        Integer maxFetchSize = HapiProperties.getMaximumFetchSize();
        retVal.setFetchSizeDefaultMaximum(maxFetchSize);
        ourLog.info("Server configured to have a maximum fetch size of "
                + (maxFetchSize == Integer.MAX_VALUE ? "'unlimited'" : maxFetchSize));

        Long reuseCachedSearchResultsMillis = HapiProperties.getReuseCachedSearchResultsMillis();
        retVal.setReuseCachedSearchResultsForMillis(reuseCachedSearchResultsMillis);
        ourLog.info("Server configured to cache search results for {} milliseconds", reuseCachedSearchResultsMillis);

        return retVal;
    }

    @Bean
    public ModelConfig modelConfig() {
        ModelConfig modelConfig = new ModelConfig();
        modelConfig.setAllowContainsSearches(this.allowContainsSearches);
        modelConfig.setAllowExternalReferences(this.allowExternalReferences);
        modelConfig.setDefaultSearchParamsCanBeOverridden(this.allowOverrideDefaultSearchParams);
        modelConfig.setEmailFromAddress(this.emailFrom);

        return modelConfig;
    }

    @Bean(destroyMethod = "close")
    public BasicDataSource dataSource() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException {
        BasicDataSource retVal = new BasicDataSource();
        Driver driver = (Driver) Class.forName(HapiProperties.getDataSourceDriver()).getConstructor().newInstance();
        retVal.setDriver(driver);
        retVal.setUrl(HapiProperties.getDataSourceUrl());
        retVal.setUsername(HapiProperties.getDataSourceUsername());
        retVal.setPassword(HapiProperties.getDataSourcePassword());
        retVal.setMaxTotal(HapiProperties.getDataSourceMaxPoolSize());
        return retVal;
    }

    /**
     * Do some fancy logging to create a nice access log that has details about each
     * incoming request.
     */
    public LoggingInterceptor loggingInterceptor() {
        LoggingInterceptor retVal = new LoggingInterceptor();
        retVal.setLoggerName(HapiProperties.getLoggerName());
        retVal.setMessageFormat(HapiProperties.getLoggerFormat());
        retVal.setErrorMessageFormat(HapiProperties.getLoggerErrorFormat());
        retVal.setLogExceptions(HapiProperties.getLoggerLogExceptions());
        return retVal;
    }

    /**
     * This interceptor adds some pretty syntax highlighting in responses when a
     * browser is detected
     */
    @Bean(autowire = Autowire.BY_TYPE)
    public ResponseHighlighterInterceptor responseHighlighterInterceptor() {
        return new ResponseHighlighterInterceptor();
    }

    @Bean
	public PartitionSettings partitionSettings() {
		return new PartitionSettings();
    }
    
    @Bean()
    public ProviderConfiguration providerConfiguration() {
        return new ProviderConfiguration(
                HapiProperties.getCdsHooksFhirServerExpandValueSets(),
                HapiProperties.getCdsHooksFhirServerMaxCodesPerQuery(),
                HapiProperties.getCdsHooksFhirServerSearchStyleEnum(),
                HapiProperties.getCdsHooksPreFetchMaxUriLength());
    }

    @Bean()
    public SearchParameterResolver searchParameterResolver(FhirContext fhirContext) {
        return new SearchParameterResolver(fhirContext);
    }

    @Bean
    public IResourceChangeListener valueSetChangeListener(IResourceChangeListenerRegistry resourceChangeListenerRegistry, Map<String, Iterable<Code>> terminologyCache) {
        IResourceChangeListener listener = new IResourceChangeListener(){

            @Override
            public void handleInit(Collection<IIdType> theResourceIds) {
                // Intentionally empty
            }

            // TODO: Selectively clear by url. Requires a lookup on the resource
            @Override
            public void handleChange(IResourceChangeEvent theResourceChangeEvent) {
                terminologyCache.clear();
            }
            
        };

        resourceChangeListenerRegistry.registerResourceResourceChangeListener("ValueSet", SearchParameterMap.newSynchronous(), listener, 1000);

        return listener;
    }

    @Bean
    public Map<String, Iterable<Code>> terminologyCache() {
        Cache<String, Iterable<Code>> cache = Caffeine.newBuilder().maximumSize(100).expireAfterAccess(Duration.ofMinutes(60)).build();
        return cache.asMap();
    }


    @Bean(name="globalModelCache")
	Map<VersionedIdentifier, Model> globalModelCache() {
		return new ConcurrentHashMap<VersionedIdentifier, Model>();
	}

	@Bean(name="globalLibraryCache") 
	Map<org.cqframework.cql.elm.execution.VersionedIdentifier, Library> globalLibraryCache() {
		return new ConcurrentHashMap<org.cqframework.cql.elm.execution.VersionedIdentifier, Library>();
	}
}
