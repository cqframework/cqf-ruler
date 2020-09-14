package org.opencds.cqf.r4.servlet;

import java.util.Arrays;

import javax.servlet.ServletException;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.common.config.HapiProperties;
import org.opencds.cqf.common.evaluation.EvaluationProviderFactory;
import org.opencds.cqf.common.retrieve.JpaFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.tooling.library.r4.NarrativeProvider;
import org.opencds.cqf.tooling.measure.r4.CodeTerminologyRef;
import org.opencds.cqf.tooling.measure.r4.CqfMeasure;
import org.opencds.cqf.tooling.measure.r4.PopulationCriteriaMap;
import org.opencds.cqf.tooling.measure.r4.VersionedTerminologyRef;
import org.opencds.cqf.r4.evaluation.ProviderFactory;
import org.opencds.cqf.r4.providers.*;
import org.springframework.context.ApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.BaseJpaResourceProvider;
import ca.uhn.fhir.jpa.provider.TerminologyUploaderProvider;
import ca.uhn.fhir.jpa.provider.r4.JpaConformanceProviderR4;
import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.jpa.rp.r4.LibraryResourceProvider;
import ca.uhn.fhir.jpa.rp.r4.MeasureResourceProvider;
import ca.uhn.fhir.jpa.rp.r4.ValueSetResourceProvider;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.searchparam.registry.ISearchParamRegistry;
import ca.uhn.fhir.jpa.term.api.ITermReadSvcR4;
import ca.uhn.fhir.jpa.api.rp.ResourceProviderFactory;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

public class BaseServlet extends RestfulServer {
    private static final long serialVersionUID = 1L;
    DaoRegistry registry;
    FhirContext fhirContext;

    @SuppressWarnings("unchecked")
    @Override
    protected void initialize() throws ServletException {
        super.initialize();

        // System level providers
        ApplicationContext appCtx = (ApplicationContext) getServletContext()
                .getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");

        // Fhir Context
        this.fhirContext = appCtx.getBean(FhirContext.class);
        this.fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        this.fhirContext.registerCustomType(VersionedTerminologyRef.class);
        this.fhirContext.registerCustomType(CodeTerminologyRef.class);
        this.fhirContext.registerCustomType(PopulationCriteriaMap.class);
        this.fhirContext.registerCustomType(CqfMeasure.class);
        setFhirContext(this.fhirContext);


        // System and Resource Daos
        IFhirSystemDao<Bundle, Meta> systemDao = appCtx.getBean("mySystemDaoR4", IFhirSystemDao.class);
        this.registry = appCtx.getBean(DaoRegistry.class);

        // System and Resource Providers
        Object systemProvider = appCtx.getBean("mySystemProviderR4", JpaSystemProviderR4.class);
        registerProvider(systemProvider);


        ResourceProviderFactory resourceProviders = appCtx.getBean("myResourceProvidersR4", ResourceProviderFactory.class);
        registerProviders(resourceProviders.createProviders());

        if(HapiProperties.getOAuthEnabled()) {
            OAuthProvider oauthProvider = new OAuthProvider(this, systemDao,
                    appCtx.getBean(DaoConfig.class), appCtx.getBean(ISearchParamRegistry.class));
            this.registerProvider(oauthProvider);
            this.setServerConformanceProvider(oauthProvider);
        }else {
            JpaConformanceProviderR4 confProvider = new JpaConformanceProviderR4(this, systemDao,
                    appCtx.getBean(DaoConfig.class), appCtx.getBean(ISearchParamRegistry.class));
            confProvider.setImplementationDescription("CQF Ruler FHIR R4 Server");
            setServerConformanceProvider(confProvider);
        }

        JpaTerminologyProvider localSystemTerminologyProvider = new JpaTerminologyProvider(
                appCtx.getBean("terminologyService", ITermReadSvcR4.class), getFhirContext(),
                (ValueSetResourceProvider) this.getResourceProvider(ValueSet.class));
        EvaluationProviderFactory providerFactory = new ProviderFactory(this.fhirContext, this.registry, localSystemTerminologyProvider);

        resolveProviders(providerFactory, localSystemTerminologyProvider, this.registry);

        // CdsHooksServlet.provider = provider;

        /*
         * ETag Support
         */
        setETagSupport(HapiProperties.getEtagSupport());

        /*
         * This server tries to dynamically generate narratives
         */
        FhirContext ctx = getFhirContext();
        ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());

        /*
         * Default to JSON and pretty printing
         */
        setDefaultPrettyPrint(HapiProperties.getDefaultPrettyPrint());

        /*
         * Default encoding
         */
        setDefaultResponseEncoding(HapiProperties.getDefaultEncoding());

        /*
         * This configures the server to page search results to and from the database,
         * instead of only paging them to memory. This may mean a performance hit when
         * performing searches that return lots of results, but makes the server much
         * more scalable.
         */
        setPagingProvider(appCtx.getBean(DatabaseBackedPagingProvider.class));

        /*
         * This interceptor formats the output using nice colourful HTML output when the
         * request is detected to come from a browser.
         */
        ResponseHighlighterInterceptor responseHighlighterInterceptor = appCtx
                .getBean(ResponseHighlighterInterceptor.class);
        this.registerInterceptor(responseHighlighterInterceptor);

        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        this.registerInterceptor(loggingInterceptor);

        /*
         * If you are hosting this server at a specific DNS name, the server will try to
         * figure out the FHIR base URL based on what the web container tells it, but
         * this doesn't always work. If you are setting links in your search bundles
         * that just refer to "localhost", you might want to use a server address
         * strategy:
         */
        String serverAddress = HapiProperties.getServerAddress();
        if (serverAddress != null && serverAddress.length() > 0) {
            setServerAddressStrategy(new HardcodedServerAddressStrategy(serverAddress));
        }

        registerProvider(appCtx.getBean(TerminologyUploaderProvider.class));

        if (HapiProperties.getCorsEnabled()) {
            CorsConfiguration config = new CorsConfiguration();
            config.addAllowedHeader("x-fhir-starter");
            config.addAllowedHeader("Origin");
            config.addAllowedHeader("Accept");
            config.addAllowedHeader("X-Requested-With");
            config.addAllowedHeader("Content-Type");
            config.addAllowedHeader("Authorization");
            config.addAllowedHeader("Cache-Control");

            config.addAllowedOrigin(HapiProperties.getCorsAllowedOrigin());

            config.addExposedHeader("Location");
            config.addExposedHeader("Content-Location");
            config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

            // Create the interceptor and register it
            CorsInterceptor interceptor = new CorsInterceptor(config);
            registerInterceptor(interceptor);
        }
    }

    protected NarrativeProvider getNarrativeProvider() {
        return new NarrativeProvider();
    }

    // Since resource provider resolution not lazy, the providers here must be
    // resolved in the correct
    // order of dependencies.
    @SuppressWarnings("unchecked")
    private void resolveProviders(EvaluationProviderFactory providerFactory,
            JpaTerminologyProvider localSystemTerminologyProvider, DaoRegistry registry) throws ServletException {
        NarrativeProvider narrativeProvider = this.getNarrativeProvider();
        HQMFProvider hqmfProvider = new HQMFProvider();

        // Code System Update
        CodeSystemUpdateProvider csUpdate = new CodeSystemUpdateProvider(this.getDao(ValueSet.class), this.getDao(CodeSystem.class));
        this.registerProvider(csUpdate);

        // Cache Value Sets
        CacheValueSetsProvider cvs = new CacheValueSetsProvider(this.registry.getSystemDao(), this.getDao(Endpoint.class));
        this.registerProvider(cvs);

        // Library processing
        LibraryOperationsProvider libraryProvider = new LibraryOperationsProvider(
                (LibraryResourceProvider) this.getResourceProvider(Library.class), narrativeProvider, registry, localSystemTerminologyProvider);
        this.registerProvider(libraryProvider);

        // CQL Execution
        CqlExecutionProvider cql = new CqlExecutionProvider(libraryProvider, providerFactory, this.fhirContext);
        this.registerProvider(cql);

        // Bundle processing
        ApplyCqlOperationProvider bundleProvider = new ApplyCqlOperationProvider(providerFactory,
                this.getDao(Bundle.class), this.fhirContext);
        this.registerProvider(bundleProvider);

        // Measure processing
        MeasureOperationsProvider measureProvider = new MeasureOperationsProvider(this.registry, providerFactory,
                narrativeProvider, hqmfProvider, libraryProvider,
                (MeasureResourceProvider) this.getResourceProvider(Measure.class));
        this.registerProvider(measureProvider);

        // // ActivityDefinition processing
        ActivityDefinitionApplyProvider actDefProvider = new ActivityDefinitionApplyProvider(this.fhirContext, cql,
                this.getDao(ActivityDefinition.class));
        this.registerProvider(actDefProvider);

        JpaFhirRetrieveProvider localSystemRetrieveProvider = new JpaFhirRetrieveProvider(registry,
                new SearchParameterResolver(this.fhirContext));

        // PlanDefinition processing
        PlanDefinitionApplyProvider planDefProvider = new PlanDefinitionApplyProvider(this.fhirContext, actDefProvider,
                this.getDao(PlanDefinition.class), this.getDao(ActivityDefinition.class), cql);
        this.registerProvider(planDefProvider);

        CdsHooksServlet.setPlanDefinitionProvider(planDefProvider);
        CdsHooksServlet.setLibraryResolutionProvider(libraryProvider);
        CdsHooksServlet.setSystemTerminologyProvider(localSystemTerminologyProvider);
        CdsHooksServlet.setSystemRetrieveProvider(localSystemRetrieveProvider);

        // QuestionnaireResponse processing
        if(HapiProperties.getQuestionnaireResponseExtractEnabled()) {
            QuestionnaireProvider questionnaireProvider = new QuestionnaireProvider(this.fhirContext);
            this.registerProvider(questionnaireProvider);
        }
        // Observation processing
        if(HapiProperties.getObservationTransformEnabled()) {
            ObservationProvider observationProvider = new ObservationProvider(this.fhirContext);
            this.registerProvider(observationProvider);
        }
    }

    protected <T extends IBaseResource> IFhirResourceDao<T> getDao(Class<T> clazz) {
        return this.registry.getResourceDao(clazz);
    }

    @SuppressWarnings("unchecked")
    protected <T extends IBaseResource> BaseJpaResourceProvider<T> getResourceProvider(Class<T> clazz) {
        return (BaseJpaResourceProvider<T>) this.getResourceProviders().stream()
                .filter(x -> x.getResourceType().getSimpleName().equals(clazz.getSimpleName())).findFirst().get();
    }
}
