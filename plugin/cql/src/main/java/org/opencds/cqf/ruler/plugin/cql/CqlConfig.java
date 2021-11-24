package org.opencds.cqf.ruler.plugin.cql;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.opencds.cqf.cql.engine.fhir.model.Dstu2FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.cql2elm.model.CacheAwareModelManager;
import org.opencds.cqf.cql.evaluator.engine.model.CachingModelResolverDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.cache.IResourceChangeListenerRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.starter.annotations.OnDSTU2Condition;
import ca.uhn.fhir.jpa.starter.annotations.OnDSTU3Condition;
import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import ca.uhn.fhir.jpa.starter.annotations.OnR5Condition;
import ca.uhn.fhir.jpa.term.api.ITermReadSvc;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.cql", name = "enabled", havingValue = "true")
public class CqlConfig {

    private static final Logger ourLog = LoggerFactory.getLogger(CqlConfig.class);

    @Autowired
    public CqlProperties cqlProperties;

    @Bean
    public CqlTranslatorOptions cqlTranslatorOptions(FhirContext fhirContext) {
        CqlTranslatorOptions options = cqlProperties.getCqlTranslatorOptions();

        if (fhirContext.getVersion().getVersion().isOlderThan(FhirVersionEnum.R4)
                && (options.getCompatibilityLevel() == "1.5" || options.getCompatibilityLevel() == "1.4")) {
            ourLog.warn(
                    "This server is configured to use CQL version > 1.4 and FHIR version <= DSTU3. Most available CQL content for DSTU3 and below is for CQL versions 1.3 or 1.4. If your CQL content causes translation errors, try setting the CQL compatibility level to 1.3");
        }

        return options;
    }

    @Bean
    public ModelManager modelManager(
            Map<org.hl7.elm.r1.VersionedIdentifier, org.cqframework.cql.cql2elm.model.Model> globalModelCache) {
        return new CacheAwareModelManager(globalModelCache);
    }

    @Bean
    public LibraryManager libraryManager(ModelManager modelManager) {
        LibraryManager libraryManager = new LibraryManager(modelManager);
        if (!cqlProperties.getUse_embedded_cql_translator_content()) {
            libraryManager.getLibrarySourceLoader().clearProviders();
        }

        return libraryManager;
    }

    // @Bean
    // public LibraryLoader libraryLoader(CqlTranslatorOptions cqlTranslatorOptions, LibraryManager libraryManager, ModelManager modelManager, Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> globalLibraryCache) {
    //     TranslatingLibraryLoader translatingLibraryLoader = new TranslatingLibraryLoader(modelManager, contentProviders,
    //             cqlTranslatorOptions);

    //     // TOOO: Need the bug fixes in the translator / evaluator to correctly detect whether the translator options match.
    //     return new CacheAwareLibraryLoaderDecorator(translatingLibraryLoader, globalLibraryCache) {
    //         @Override
    //         protected Boolean translatorOptionsMatch(org.cqframework.cql.elm.execution.Library library) {
    //             return true;
    //         }
    //     };
    // }

    @Bean
    SearchParameterResolver searchParameterResolver(FhirContext fhirContext) {
        return new SearchParameterResolver(fhirContext);
    }

    @Bean
    JpaFhirRetrieveProvider jpaFhirRetrieveProvider(DaoRegistry daoRegistry,
            SearchParameterResolver searchParameterResolver) {
        return new JpaFhirRetrieveProvider(daoRegistry, searchParameterResolver);
    }

    @Bean
    JpaTerminologyProvider jpaTerminologyProvider(ITermReadSvc theTerminologySvc, DaoRegistry theDaoRegistry,
            IValidationSupport theValidationSupport) {
        return new JpaTerminologyProvider(theTerminologySvc, theDaoRegistry, theValidationSupport);
    }

    // TODO: Use something like caffeine caching for this so that growth is limited.
    @Bean
    public Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> globalLibraryCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Map<org.hl7.elm.r1.VersionedIdentifier, org.cqframework.cql.cql2elm.model.Model> globalModelCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public IFhirResourceDao<?> libraryDao(DaoRegistry daoRegistry) {
        return daoRegistry.getResourceDao("Library");
    }

    @Bean
    public ElmCacheResourceChangeListener elmCacheResourceChangeListener(
            IResourceChangeListenerRegistry resourceChangeListenerRegistry, IFhirResourceDao<?> libraryDao,
            Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> globalLibraryCache) {
        ElmCacheResourceChangeListener listener = new ElmCacheResourceChangeListener(libraryDao, globalLibraryCache);
        resourceChangeListenerRegistry.registerResourceResourceChangeListener("Library",
                SearchParameterMap.newSynchronous(), listener, 1000);
        return listener;
    }

    @Bean
    @Conditional(OnDSTU2Condition.class)
    public ModelResolver modelResolverDstu2(FhirContext fhirContext) {
        if (fhirContext.getVersion().getVersion() == FhirVersionEnum.DSTU2_1
                || fhirContext.getVersion().getVersion() == FhirVersionEnum.DSTU2_HL7ORG) {
            throw new IllegalStateException(
                    "CQL support not yet implemented for DSTU2_1 or DSTU2_HL7ORG. Please use DSTU2 or disable the CQL plugin.");
        }

        return new CachingModelResolverDecorator(new Dstu2FhirModelResolver());
    }

    @Bean
    @Conditional(OnDSTU3Condition.class)
    public ModelResolver modelResolverDstu3() {
        return new CachingModelResolverDecorator(new Dstu3FhirModelResolver());
    }

    @Bean
    @Conditional(OnR4Condition.class)
    public ModelResolver modelResolverR4() {
        return new CachingModelResolverDecorator(new R4FhirModelResolver());
    }

    @Bean
    @Conditional(OnR5Condition.class)
    public ModelResolver modelResolverR5() {
        throw new IllegalStateException("CQL support not yet implemented for R5. Please disable the CQL plugin.");
    }
}
