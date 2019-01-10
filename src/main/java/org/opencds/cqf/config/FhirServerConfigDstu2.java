package org.opencds.cqf.config;

import ca.uhn.fhir.jpa.config.BaseJavaConfigDstu2;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.search.LuceneSearchMappingFactory;
import ca.uhn.fhir.rest.server.interceptor.RequestValidatingInterceptor;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import org.apache.commons.dbcp2.BasicDataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;
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
import java.util.Properties;

@Configuration
@Import(FhirServerConfigCommon.class)
@EnableTransactionManagement()
public class FhirServerConfigDstu2 extends BaseJavaConfigDstu2 {

    @Bean()
    public DaoConfig daoConfig() {
        DaoConfig retVal = new DaoConfig();
        retVal.setAllowMultipleDelete(true);
        retVal.setAllowInlineMatchUrlReferences(true);
        retVal.setAllowExternalReferences(true);
        retVal.getTreatBaseUrlsAsLocal().add("http://measure.eval.kanvix.com/cqf-ruler/baseDstu2");
        retVal.getTreatBaseUrlsAsLocal().add("https://measure.eval.kanvix.com/cqf-ruler/baseDstu2");
        retVal.setCountSearchResultsUpTo(50000);
        retVal.setIndexMissingFields(DaoConfig.IndexEnabledEnum.ENABLED);
        retVal.setFetchSizeDefaultMaximum(50000);
        retVal.setAllowMultipleDelete(true);
        return retVal;
    }

    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean retVal = new LocalContainerEntityManagerFactoryBean();
        retVal.setPersistenceUnitName("PU_HapiFhirJpaDstu2");
        retVal.setDataSource(dataSource());
        retVal.setPackagesToScan("ca.uhn.fhir.jpa.entity");
        retVal.setPersistenceProvider(new HibernatePersistenceProvider());
        retVal.setJpaProperties(jpaProperties());
        return retVal;
    }

    @Bean
    @Lazy
    public RequestValidatingInterceptor requestValidatingInterceptor() {
        RequestValidatingInterceptor requestValidator = new RequestValidatingInterceptor();
        requestValidator.setFailOnSeverity(null);
        requestValidator.setAddResponseHeaderOnSeverity(null);
        requestValidator.setAddResponseOutcomeHeaderOnSeverity(ResultSeverityEnum.INFORMATION);
        requestValidator.addValidatorModule(instanceValidatorDstu2());
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

//    @Bean
//    public IServerInterceptor securityInterceptor() {
//        return new PublicSecurityInterceptor();
//    }

    // Derby config
    @Bean(name = "myPersistenceDataSourceDstu2", destroyMethod = "close")
    public DataSource dataSource() {
        BasicDataSource retVal = new BasicDataSource();
        retVal.setDriver(new org.apache.derby.jdbc.EmbeddedDriver());
        retVal.setUrl("jdbc:derby:directory:target/dstu2;create=true");
        retVal.setUsername("");
        retVal.setPassword("");
        return retVal;
    }

    // Derby config
    private Properties jpaProperties() {
        Properties extraProperties = new Properties();
        extraProperties.put("hibernate.dialect", org.hibernate.dialect.DerbyTenSevenDialect.class.getName());
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
        extraProperties.put("hibernate.search.default.indexBase", "target/lucenefiles_dstu2");
        extraProperties.put("hibernate.search.lucene_version", "LUCENE_CURRENT");
//		extraProperties.put("hibernate.search.default.worker.execution", "async");
        return extraProperties;
    }

    // H2 Config
//    @Bean(name = "myPersistenceDataSourceDstu2", destroyMethod = "close")
//    public DataSource dataSource() {
//        Path path = Paths.get("target/stu2").toAbsolutePath();
//        BasicDataSource retVal = new BasicDataSource();
//        retVal.setDriver(new org.h2.Driver());
//        retVal.setUrl("jdbc:h2:file:" + path.toString() + ";create=true;MV_STORE=FALSE;MVCC=FALSE");
//        retVal.setUsername("");
//        retVal.setPassword("");
//        return retVal;
//    }

    // H2 config
//    private Properties jpaProperties() {
//        Properties extraProperties = new Properties();
//        extraProperties.put("hibernate.dialect", org.hibernate.dialect.H2Dialect.class.getName());
//        extraProperties.put("hibernate.format_sql", "true");
//        extraProperties.put("hibernate.show_sql", "false");
//        extraProperties.put("hibernate.hbm2ddl.auto", "update");
//        extraProperties.put("hibernate.jdbc.batch_size", "20");
//        extraProperties.put("hibernate.cache.use_query_cache", "false");
//        extraProperties.put("hibernate.cache.use_second_level_cache", "false");
//        extraProperties.put("hibernate.cache.use_structured_entries", "false");
//        extraProperties.put("hibernate.cache.use_minimal_puts", "false");
//        extraProperties.put("hibernate.search.model_mapping", LuceneSearchMappingFactory.class.getName());
//        extraProperties.put("hibernate.search.default.directory_provider", "filesystem");
//        extraProperties.put("hibernate.search.default.indexBase", "target/lucenefiles_dstu2");
//        extraProperties.put("hibernate.search.lucene_version", "LUCENE_CURRENT");
////		extraProperties.put("hibernate.search.default.worker.execution", "async");
//        return extraProperties;
//    }

}
