package org.opencds.cqf.config;

import ca.uhn.fhir.jpa.config.BaseJavaConfigR4;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.search.LuceneSearchMappingFactory;
import ca.uhn.fhir.rest.server.interceptor.RequestValidatingInterceptor;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import org.apache.commons.dbcp2.BasicDataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Configuration
@Import(FhirServerConfigCommon.class)
@EnableTransactionManagement()
public class FhirServerConfigR4 extends BaseJavaConfigR4 {

    @Bean()
    public DaoConfig daoConfig() {
        DaoConfig retVal = new DaoConfig();
        retVal.setAllowMultipleDelete(true);
        retVal.setAllowInlineMatchUrlReferences(true);
        retVal.setAllowExternalReferences(true);
        retVal.getTreatBaseUrlsAsLocal().add("http://measure.eval.kanvix.com/cqf-ruler/baseR4");
        retVal.getTreatBaseUrlsAsLocal().add("https://measure.eval.kanvix.com/cqf-ruler/baseR4");
        retVal.setCountSearchResultsUpTo(50000);
        retVal.setIndexMissingFields(DaoConfig.IndexEnabledEnum.ENABLED);
        retVal.setFetchSizeDefaultMaximum(10000);
        retVal.setAllowMultipleDelete(true);
        return retVal;
    }

    // H2 Config
    @Bean(name = "myPersistenceDataSourceR4", destroyMethod = "close")
    public DataSource dataSource() {
        Path path = Paths.get("target/r4").toAbsolutePath();
        BasicDataSource retVal = new BasicDataSource();
        retVal.setDriver(new org.h2.Driver());
        retVal.setUrl("jdbc:h2:file:" + path.toString() + ";create=true;MV_STORE=FALSE;MVCC=FALSE");
        retVal.setUsername("");
        retVal.setPassword("");
        return retVal;
    }

    @Override
    @Bean(autowire = Autowire.BY_TYPE)
    public DatabaseBackedPagingProvider databaseBackedPagingProvider() {
        DatabaseBackedPagingProvider retVal = super.databaseBackedPagingProvider();
        retVal.setDefaultPageSize(20);
        retVal.setMaximumPageSize(500);
        return retVal;
    }

    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean retVal = new LocalContainerEntityManagerFactoryBean();
        retVal.setPersistenceUnitName("PU_HapiFhirJpaR4");
        retVal.setDataSource(dataSource());
        retVal.setPackagesToScan("ca.uhn.fhir.jpa.entity");
        retVal.setPersistenceProvider(new HibernatePersistenceProvider());
        retVal.setJpaProperties(jpaProperties());
        return retVal;
    }

    // H2 config
    private Properties jpaProperties() {
        Properties extraProperties = new Properties();
        extraProperties.put("hibernate.dialect", org.hibernate.dialect.H2Dialect.class.getName());
        extraProperties.put("hibernate.format_sql", "true");
        extraProperties.put("hibernate.show_sql", "false");
        extraProperties.put("hibernate.hbm2ddl.auto", "update");
        extraProperties.put("hibernate.jdbc.batch_size", "20");
        extraProperties.put("hibernate.cache.use_query_cache", "false");
        extraProperties.put("hibernate.cache.use_second_level_cache", "false");
        extraProperties.put("hibernate.cache.use_structured_entries", "false");
        extraProperties.put("hibernate.cache.use_minimal_puts", "false");
        extraProperties.put("hibernate.search.model_mapping", LuceneSearchMappingFactory.class.getName());
        extraProperties.put("hibernate.search.default.directory_provider", "filesystem");
        extraProperties.put("hibernate.search.default.indexBase", "target/lucenefiles_r4");
        extraProperties.put("hibernate.search.lucene_version", "LUCENE_CURRENT");
//		extraProperties.put("hibernate.search.default.worker.execution", "async");
        return extraProperties;
    }

    @Bean
    @Lazy
    public RequestValidatingInterceptor requestValidatingInterceptor() {
        RequestValidatingInterceptor requestValidator = new RequestValidatingInterceptor();
        requestValidator.setFailOnSeverity(null);
        requestValidator.setAddResponseHeaderOnSeverity(null);
        requestValidator.setAddResponseOutcomeHeaderOnSeverity(ResultSeverityEnum.INFORMATION);
        requestValidator.addValidatorModule(instanceValidatorR4());
        requestValidator.setIgnoreValidatorExceptions(true);

        return requestValidator;
    }

    @Bean()
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager retVal = new JpaTransactionManager();
        retVal.setEntityManagerFactory(entityManagerFactory);
        return retVal;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
