package org.opencds.cqf.ruler.cql;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.quick.FhirLibrarySourceProvider;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.fhir.model.Dstu2FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.evaluator.CqlOptions;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.DataProviderComponents;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.cql2elm.model.CacheAwareModelManager;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.execution.CacheAwareLibraryLoaderDecorator;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader;
import org.opencds.cqf.cql.evaluator.engine.model.CachingModelResolverDecorator;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.spring.fhir.adapter.AdapterConfiguration;
import org.opencds.cqf.external.annotations.OnDSTU2Condition;
import org.opencds.cqf.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.external.annotations.OnR4Condition;
import org.opencds.cqf.external.annotations.OnR5Condition;
import org.opencds.cqf.ruler.cql.dstu2.PreExpandedTermReadSvcDstu2;
import org.opencds.cqf.ruler.cql.dstu3.PreExpandedTermReadSvcDstu3;
import org.opencds.cqf.ruler.cql.interceptor.CqlExceptionHandlingInterceptor;
import org.opencds.cqf.ruler.cql.r4.PreExpandedTermReadSvcR4;
import org.opencds.cqf.ruler.cql.r5.PreExpandedTermReadSvcR5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.core.context.SecurityContextHolder;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.cql.common.provider.CqlProviderFactory;
import ca.uhn.fhir.cql.common.provider.CqlProviderLoader;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoValueSet;
import ca.uhn.fhir.jpa.cache.IResourceChangeListenerRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.term.api.ITermReadSvc;
import ca.uhn.fhir.jpa.term.api.ITermReadSvcDstu3;
import ca.uhn.fhir.jpa.term.api.ITermReadSvcR4;
import ca.uhn.fhir.jpa.term.api.ITermReadSvcR5;
import ca.uhn.fhir.jpa.validation.JpaValidationSupportChain;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.cql", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(AdapterConfiguration.class)
public class CqlConfig {

	private static final Logger ourLog = LoggerFactory.getLogger(CqlConfig.class);

	@Bean
	public CqlProperties cqlProperties() {
		return new CqlProperties();
	}

	@Bean
	public CqlOptions cqlOptions() {
		return cqlProperties().getOptions();
	}

	@Bean
	public CqlExceptionHandlingInterceptor cqlExceptionHandlingInterceptor() {
		return new CqlExceptionHandlingInterceptor();
	}

	@Bean
	public CqlTranslatorOptions cqlTranslatorOptions(FhirContext fhirContext, CqlProperties cqlProperties) {
		CqlTranslatorOptions options = cqlProperties.getOptions().getCqlTranslatorOptions();

		if (fhirContext.getVersion().getVersion().isOlderThan(FhirVersionEnum.R4)
				&& (options.getCompatibilityLevel().equals("1.5") || options.getCompatibilityLevel().equals("1.4"))) {
			ourLog.warn("{} {} {}",
					"This server is configured to use CQL version > 1.4 and FHIR version <= DSTU3.",
					"Most available CQL content for DSTU3 and below is for CQL versions 1.3.",
					"If your CQL content causes translation errors, try setting the CQL compatibility level to 1.3");
		}

		return options;
	}

	@Bean
	public ModelManager modelManager(
			Map<org.hl7.cql.model.ModelIdentifier, org.cqframework.cql.cql2elm.model.Model> globalModelCache) {
		return new CacheAwareModelManager(globalModelCache);
	}

	@Bean
	public LibraryManagerFactory libraryManagerFactory(
			ModelManager modelManager) {
		return (providers) -> {
			LibraryManager libraryManager = new LibraryManager(modelManager);
			for (LibrarySourceProvider provider : providers) {
				libraryManager.getLibrarySourceLoader().registerProvider(provider);
			}
			return libraryManager;
		};
	}

	@Bean
	public SearchParameterResolver searchParameterResolver(FhirContext fhirContext) {
		return new SearchParameterResolver(fhirContext);
	}

	@Bean
	JpaFhirDalFactory jpaFhirDalFactory(DaoRegistry daoRegistry) {
		return rd -> new JpaFhirDal(daoRegistry, rd);
	}

	@Bean
	JpaDataProviderFactory jpaDataProviderFactory(ModelResolver modelResolver, DaoRegistry daoRegistry,
			SearchParameterResolver searchParameterResolver) {
		return (rd, t) -> {
			JpaFhirRetrieveProvider provider = new JpaFhirRetrieveProvider(daoRegistry, searchParameterResolver, rd);
			if (t != null) {
				provider.setTerminologyProvider(t);
				provider.setExpandValueSets(true);
				provider.setMaxCodesPerQuery(2048);
				provider.setQueryBatchThreshold(5);
				provider.setModelResolver(modelResolver);
			}
			return new CompositeDataProvider(modelResolver, provider);
		};
	}

	@Bean
	DataProviderFactory dataProviderFactory(FhirContext fhirContext, ModelResolver modelResolver) {
		return new DataProviderFactory() {
			@Override
			public DataProviderComponents create(EndpointInfo endpointInfo) {
				// to do implement endpoint
				return null;
			}

			@Override
			public DataProviderComponents create(IBaseBundle dataBundle) {
				return new DataProviderComponents(Constants.FHIR_MODEL_URI, modelResolver,
						new BundleRetrieveProvider(fhirContext, dataBundle));
			}
		};

	}

	@Bean
	public JpaFhirRetrieveProvider jpaFhirRetrieveProvider(DaoRegistry daoRegistry,
			SearchParameterResolver searchParameterResolver) {
		return new JpaFhirRetrieveProvider(daoRegistry, searchParameterResolver);
	}

	@SuppressWarnings("unchecked")
	@Bean
	IFhirResourceDaoValueSet<IBaseResource, ICompositeType, ICompositeType> valueSetDao(DaoRegistry daoRegistry) {
		return (IFhirResourceDaoValueSet<IBaseResource, ICompositeType, ICompositeType>) daoRegistry
				.getResourceDao("ValueSet");
	}

	@Bean
	public JpaTerminologyProviderFactory jpaTerminologyProviderFactory(ITermReadSvc theTerminologySvc,
			IValidationSupport theValidationSupport,
			Map<org.cqframework.cql.elm.execution.VersionedIdentifier, List<Code>> globalCodeCache) {
		return rd -> new JpaTerminologyProvider(theTerminologySvc, theValidationSupport, globalCodeCache,
				rd);
	}

	@Bean
	JpaLibrarySourceProviderFactory jpaLibrarySourceProviderFactory(DaoRegistry daoRegistry) {
		return rd -> new JpaLibrarySourceProvider(daoRegistry, rd);
	}

	@Bean
	LibraryLoaderFactory libraryLoaderFactory(
			Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> globalLibraryCache,
			ModelManager modelManager, CqlTranslatorOptions cqlTranslatorOptions, CqlProperties cqlProperties) {
		return lcp -> {

			if (cqlProperties.getOptions().useEmbeddedLibraries()) {
				lcp.add(new FhirLibrarySourceProvider());
			}

			return new CacheAwareLibraryLoaderDecorator(
					new TranslatingLibraryLoader(modelManager, lcp, cqlTranslatorOptions, null), globalLibraryCache) {
				// TODO: This is due to a bug with the ELM annotations which prevent options
				// from matching the way they should
				@Override
				protected Boolean translatorOptionsMatch(org.cqframework.cql.elm.execution.Library library) {
					return true;
				}
			};
		};
	}

	// TODO: Use something like caffeine caching for this so that growth is limited.
	@Bean
	public Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> globalLibraryCache() {
		return new ConcurrentHashMap<>();
	}

	@Bean
	public Map<org.cqframework.cql.elm.execution.VersionedIdentifier, List<Code>> globalCodeCache() {
		return new ConcurrentHashMap<>();
	}

	@Bean
	public Map<org.hl7.cql.model.ModelIdentifier, org.cqframework.cql.cql2elm.model.Model> globalModelCache() {
		return new ConcurrentHashMap<>();
	}

	@Bean
	@Primary
	public ElmCacheResourceChangeListener elmCacheResourceChangeListener(
			IResourceChangeListenerRegistry resourceChangeListenerRegistry, DaoRegistry daoRegistry,
			Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> globalLibraryCache) {
		ElmCacheResourceChangeListener listener = new ElmCacheResourceChangeListener(daoRegistry, globalLibraryCache);
		resourceChangeListenerRegistry.registerResourceResourceChangeListener("Library",
				SearchParameterMap.newSynchronous(), listener, 1000);
		return listener;
	}

	@Bean
	@Primary
	public CodeCacheResourceChangeListener codeCacheResourceChangeListener(
			IResourceChangeListenerRegistry resourceChangeListenerRegistry, DaoRegistry daoRegistry,
			Map<org.cqframework.cql.elm.execution.VersionedIdentifier, List<Code>> globalCodeCache) {
		CodeCacheResourceChangeListener listener = new CodeCacheResourceChangeListener(daoRegistry, globalCodeCache);
		resourceChangeListenerRegistry.registerResourceResourceChangeListener("ValueSet",
				SearchParameterMap.newSynchronous(), listener, 1000);
		return listener;
	}

	@Bean
	@Primary
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
	@Primary
	@Conditional(OnDSTU3Condition.class)
	public ModelResolver modelResolverDstu3() {
		return new CachingModelResolverDecorator(new Dstu3FhirModelResolver());
	}

	@Bean(name = "r4ModelResolver")
	@Primary
	@Conditional(OnR4Condition.class)
	public ModelResolver modelResolverR4() {
		return new CachingModelResolverDecorator(new R4FhirModelResolver());
	}

	@Bean
	@Primary
	@Conditional(OnR5Condition.class)
	public ModelResolver modelResolverR5() {
		// TODO: The key piece missing for R5 support is a ModelInfo in the CQL
		// Translator. That's being tracked here:
		// https://github.com/cqframework/clinical_quality_language/issues/665
		throw new IllegalStateException(
				"CQL support not yet implemented for R5. Please disable the CQL plugin or switch the server to <=R4");
	}

	@Bean
	public LibraryVersionSelector libraryVersionSelector(AdapterFactory adapterFactory) {
		return new LibraryVersionSelector(adapterFactory);
	}

	// This overrides the base HAPI cql provider.
	@Bean
	@Primary
	public CqlProviderLoader cqlProviderLoader() {
		return null;
	}

	@Bean
	@Primary
	public CqlProviderFactory cqlProviderFactory() {
		return null;
	}

	@Bean({ "terminologyService", "myTerminologySvc", "myTermReadSvc" })
	@Conditional(OnDSTU2Condition.class)
	public ITermReadSvc termReadSvcDstu2() {
		return new PreExpandedTermReadSvcDstu2();
	}

	@Bean({ "terminologyService", "myTerminologySvc", "myTermReadSvc" })
	@Conditional(OnDSTU3Condition.class)
	public ITermReadSvcDstu3 termReadSvcDstu3() {
		return new PreExpandedTermReadSvcDstu3();
	}

	@Bean({ "terminologyService", "myTerminologySvc", "myTermReadSvc" })
	@Conditional(OnR4Condition.class)
	public ITermReadSvcR4 termReadSvcR4() {
		return new PreExpandedTermReadSvcR4();
	}

	@Bean({ "terminologyService", "myTerminologySvc", "myTermReadSvc" })
	@Conditional(OnR5Condition.class)
	public ITermReadSvcR5 termReadSvcR5() {
		return new PreExpandedTermReadSvcR5();
	}

	@Bean(name = "cqlExecutor")
	public Executor cqlExecutor() {
		CqlForkJoinWorkerThreadFactory factory = new CqlForkJoinWorkerThreadFactory();
		ForkJoinPool myCommonPool = new ForkJoinPool(Math.min(32767, Runtime.getRuntime().availableProcessors()),
				factory,
				null, false);

		return new DelegatingSecurityContextExecutor(myCommonPool,
				SecurityContextHolder.getContext());
	}

	@Bean
	@Lazy(false)
	public IValidationSupport preExpandedValidationSupport(JpaValidationSupportChain jpaSupportChain,
			FhirContext fhirContext) {
		var preExpandedValidationSupport = new PreExpandedValidationSupport(fhirContext);
		jpaSupportChain.addValidationSupport(0, preExpandedValidationSupport);
		return preExpandedValidationSupport;
	}
}
