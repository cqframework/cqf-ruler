package org.opencds.cqf.dstu3.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.opencds.cqf.common.config.HapiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.ParserOptions;
import ca.uhn.fhir.jpa.config.BaseJavaConfigDstu3;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;

@Configuration
public class FhirServerConfigDstu3 extends BaseJavaConfigDstu3 {
    protected final DataSource myDataSource;

    @Autowired
    public FhirServerConfigDstu3(DataSource myDataSource) {
        this.myDataSource = myDataSource;
    }

    @Override
    public FhirContext fhirContextDstu3() {
        FhirContext retVal = FhirContext.forDstu3();

        // Don't strip versions in some places
        ParserOptions parserOptions = retVal.getParserOptions();
        parserOptions.setDontStripVersionsFromReferencesAtPaths("AuditEvent.entity.reference");

        return retVal;
    }

    /**
     * We override the paging provider definition so that we can customize the
     * default/max page sizes for search results. You can set these however you
     * want, although very large page sizes will require a lot of RAM.
     */
    @Override
    public DatabaseBackedPagingProvider databaseBackedPagingProvider() {
        DatabaseBackedPagingProvider pagingProvider = super.databaseBackedPagingProvider();
        pagingProvider.setDefaultPageSize(HapiProperties.getDefaultPageSize());
        pagingProvider.setMaximumPageSize(HapiProperties.getMaximumPageSize());
        return pagingProvider;
    }

    @Override
    @Bean()
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean retVal = super.entityManagerFactory();
        retVal.setPersistenceUnitName(HapiProperties.getPersistenceUnitName());

        try {
            retVal.setDataSource(myDataSource);
        } catch (Exception e) {
            throw new ConfigurationException("Could not set the data source due to a configuration issue", e);
        }

        retVal.setJpaProperties(HapiProperties.getProperties());
        return retVal;
    }

    @Bean()
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager retVal = new JpaTransactionManager();
        retVal.setEntityManagerFactory(entityManagerFactory);
        return retVal;
    }
}
