package org.opencds.cqf.ruler.server;


import java.util.Map;

/* 
*  This file created from the HAPI FHIR JPA Server Starter project
*  https://github.com/hapifhir/hapi-fhir-jpaserver-starter 
*/

import java.util.Set;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.lang3.tuple.Triple;
import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.FhirDalFactory;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.dal.TypedFhirDalFactory;
import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.model.CachingModelResolverDecorator;
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.cql.evaluator.spring.EvaluatorConfiguration;
import org.opencds.cqf.ruler.common.dal.RulerDal;
import org.opencds.cqf.ruler.r4.config.OperationsProviderLoader;
import org.opencds.cqf.ruler.r4.helpers.LibraryHelper2;
import org.opencds.cqf.ruler.r4.providers.PlanDefinitionApplyProvider;
import org.opencds.cqf.ruler.server.annotations.OnR4Condition;
import org.opencds.cqf.ruler.server.cql.StarterCqlR4Config;
import org.opencds.cqf.ruler.server.factory.DefaultingDataProviderFactory;
import org.opencds.cqf.ruler.server.factory.DefaultingFhirDalFactory;
import org.opencds.cqf.ruler.server.factory.DefaultingLibraryLoaderFactory;
import org.opencds.cqf.ruler.server.factory.DefaultingTerminologyProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.cql.common.provider.LibraryResolutionProvider;
import ca.uhn.fhir.cql.common.retrieve.JpaFhirRetrieveProvider;
import ca.uhn.fhir.cql.r4.provider.JpaTerminologyProvider;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.config.BaseJavaConfigR4;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.search.lastn.ElasticsearchSvcImpl;

@Configuration
@Conditional(OnR4Condition.class)
@Import({StarterCqlR4Config.class, EvaluatorConfiguration.class})
public class FhirServerConfigR4 extends BaseJavaConfigR4 {

  @Autowired
  private DataSource myDataSource;

  /**
   * We override the paging provider definition so that we can customize
   * the default/max page sizes for search results. You can set these however
   * you want, although very large page sizes will require a lot of RAM.
   */
  @Autowired
  AppProperties appProperties;

  @Override
  public DatabaseBackedPagingProvider databaseBackedPagingProvider() {
    DatabaseBackedPagingProvider pagingProvider = super.databaseBackedPagingProvider();
    pagingProvider.setDefaultPageSize(appProperties.getDefault_page_size());
    pagingProvider.setMaximumPageSize(appProperties.getMax_page_size());
    return pagingProvider;
  }

  @Autowired
  private ConfigurableEnvironment configurableEnvironment;

  @Override
  @Bean()
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
    LocalContainerEntityManagerFactoryBean retVal = super.entityManagerFactory();
    retVal.setPersistenceUnitName("HAPI_PU");

    try {
      retVal.setDataSource(myDataSource);
    } catch (Exception e) {
      throw new ConfigurationException("Could not set the data source due to a configuration issue", e);
    }

    retVal.setJpaProperties(EnvironmentHelper.getHibernateProperties(configurableEnvironment));
    return retVal;
  }

  @Bean
  @Primary
  public JpaTransactionManager hapiTransactionManager(EntityManagerFactory entityManagerFactory) {
    JpaTransactionManager retVal = new JpaTransactionManager();
    retVal.setEntityManagerFactory(entityManagerFactory);
    return retVal;
  }

  @Bean()
  public ElasticsearchSvcImpl elasticsearchSvc() {
    if (EnvironmentHelper.isElasticsearchEnabled(configurableEnvironment)) {
		 String elasticsearchUrl = EnvironmentHelper.getElasticsearchServerUrl(configurableEnvironment);
		 String elasticsearchHost;
		 if (elasticsearchUrl.startsWith("http")) {
			 elasticsearchHost = elasticsearchUrl.substring(elasticsearchUrl.indexOf("://") + 3, elasticsearchUrl.lastIndexOf(":"));
		 } else {
			 elasticsearchHost = elasticsearchUrl.substring(0, elasticsearchUrl.indexOf(":"));
		 }

      String elasticsearchUsername = EnvironmentHelper.getElasticsearchServerUsername(configurableEnvironment);
      String elasticsearchPassword = EnvironmentHelper.getElasticsearchServerPassword(configurableEnvironment);
      int elasticsearchPort = Integer.parseInt(elasticsearchUrl.substring(elasticsearchUrl.lastIndexOf(":")+1));
      return new ElasticsearchSvcImpl(elasticsearchHost, elasticsearchPort, elasticsearchUsername, elasticsearchPassword);
    } else {
      return null;
    }
  }

  @Bean
  OperationsProviderLoader operationsProviderLoader() {
    return new OperationsProviderLoader();
  }

  @Bean
  PlanDefinitionApplyProvider planDefinitionApplyProvider(RulerDal fhirDal, FhirContext fhirContext, ActivityDefinitionProcessor activityDefinitionProcessor,
  LibraryProcessor libraryProcessor, IFhirResourceDao<PlanDefinition> planDefinitionDao, AdapterFactory adapterFactory, FhirTypeConverter fhirTypeConverter) {
    return new PlanDefinitionApplyProvider(fhirDal, fhirContext, activityDefinitionProcessor, libraryProcessor, planDefinitionDao, adapterFactory, fhirTypeConverter);
  }


  @Bean
  ActivityDefinitionProcessor activityDefinitionProcessor(FhirContext fhirContext, RulerDal fhirDal, LibraryProcessor libraryProcessor) {
    return new ActivityDefinitionProcessor(fhirContext, fhirDal, libraryProcessor);
  }

  @Primary
  @Bean
  TerminologyProviderFactory defaultingTerminologyProviderFactory(FhirContext fhirContext,
  Set<TypedTerminologyProviderFactory> terminologyProviderFactories, JpaTerminologyProvider jpaTerminologyProvider) {
    return new DefaultingTerminologyProviderFactory(fhirContext, terminologyProviderFactories, jpaTerminologyProvider);
  }

  @Primary
  @Bean
  DataProviderFactory defaultingDataProviderFactory(FhirContext fhirContext, Set<ModelResolverFactory> modelResolverFactories,
  Set<TypedRetrieveProviderFactory> retrieveProviderFactories, ModelResolver modelResolver, JpaFhirRetrieveProvider retrieveProvider) {
    return new DefaultingDataProviderFactory(fhirContext, modelResolverFactories, retrieveProviderFactories, Triple.of(Constants.FHIR_MODEL_URI, modelResolver, retrieveProvider));
  }

  @Primary
  @Bean
  FhirDalFactory defaultingFhirDalFactory(Set<TypedFhirDalFactory> fhirDalFactories, RulerDal rulerDal) {
    return new DefaultingFhirDalFactory(fhirDalFactories, rulerDal);
  }

  @Primary
  @Bean
  DefaultingLibraryLoaderFactory defaultingLibraryLoaderFactory(FhirContext fhirContext, AdapterFactory adapterFactory,
  Set<TypedLibraryContentProviderFactory> libraryContentProviderFactories,
  LibraryVersionSelector libraryVersionSelector, LibraryLoader libraryLoader) {
    return new DefaultingLibraryLoaderFactory(fhirContext, adapterFactory, libraryContentProviderFactories, libraryVersionSelector, libraryLoader);
  }

  @Bean
  @SuppressWarnings({"rawtypes", "unchecked"})
  LibraryLoader defaultLibraryLoader(LibraryHelper2 libraryHelper, LibraryResolutionProvider libraryResolutionProvider) {
    return libraryHelper.createLibraryLoader(libraryResolutionProvider);
  }

  @Bean
  LibraryHelper2 libraryHelper2(Map<org.hl7.elm.r1.VersionedIdentifier, Model> modelCache) {
    return new LibraryHelper2(modelCache);
  }

  @Bean
  ModelResolver modelResolver() {
    return new CachingModelResolverDecorator(new R4FhirModelResolver());
  }

  @Bean
  JpaFhirRetrieveProvider jpaFhirRetrieveProvider(FhirContext fhirContext, DaoRegistry daoRegistry, JpaTerminologyProvider jpaTerminologyProvider) {
    JpaFhirRetrieveProvider retrieveProvider = new JpaFhirRetrieveProvider(daoRegistry,
            new SearchParameterResolver(fhirContext));
    retrieveProvider.setTerminologyProvider(jpaTerminologyProvider);
    retrieveProvider.setExpandValueSets(true);
    return retrieveProvider;
  }
}
