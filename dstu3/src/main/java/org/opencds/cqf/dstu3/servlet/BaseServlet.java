package org.opencds.cqf.dstu3.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.BaseJpaResourceProvider;
import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.JpaSystemProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.TerminologyUploaderProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.*;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.term.IHapiTerminologySvcDstu3;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.config.HapiProperties;
import org.opencds.cqf.config.ResourceProviderRegistry;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.dstu3.providers.*;
import org.opencds.cqf.dstu3.config.FhirServerConfigDstu3;
import org.opencds.cqf.dstu3.evaluation.ProviderFactory;
import org.opencds.cqf.config.FhirServerConfigCommon;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class BaseServlet extends RestfulServer {
    // This registry holds a collection of all the resource providers on this
    // server.
    // This is essentially a duplicate of the resourceProviders on the underlying
    // RestfulServer
    // with the intention that both collections will be synchronized at the end of
    // the initialization
    // and the registry can be used in various classes that needs access the to
    // resource providers.
    // Additionally, it provides the functionality to resolve a specific resource
    // type from the collection.
    ResourceProviderRegistry registry;

    FhirContext fhirContext;

    @SuppressWarnings("unchecked")
    @Override
    protected void initialize() throws ServletException {
        super.initialize();

        // System level providers
        ApplicationContext appCtx = (ApplicationContext) getServletContext()
                .getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");

        this.fhirContext = appCtx.getBean(FhirContext.class);
        this.fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        setFhirContext(this.fhirContext);

        Object systemProvider = appCtx.getBean("mySystemProviderDstu3", JpaSystemProviderDstu3.class);
        registerProvider(systemProvider);

        IFhirSystemDao<Bundle, Meta> systemDao = appCtx.getBean("mySystemDaoDstu3", IFhirSystemDao.class);
        JpaConformanceProviderDstu3 confProvider = new JpaConformanceProviderDstu3(this, systemDao, appCtx.getBean(DaoConfig.class));
        confProvider.setImplementationDescription("CQF Ruler FHIR DSTU3 Server");
        setServerConformanceProvider(confProvider);

        // Resource Providers
        List<IResourceProvider> resourceProviders = appCtx.getBean("myResourceProvidersDstu3", List.class);
        this.registry = new ResourceProviderRegistry();
        for (IResourceProvider provider : resourceProviders) {
            registry.register(provider);
        }


        List<Object> plainProviders = new ArrayList<>();
        plainProviders.add(appCtx.getBean(TerminologyUploaderProviderDstu3.class));

        TerminologyProvider localSystemTerminologyProvider = new JpaTerminologyProvider(appCtx.getBean("terminologyService", IHapiTerminologySvcDstu3.class), getFhirContext(), (ValueSetResourceProvider) registry.resolve("ValueSet"));
        ProviderFactory providerFactory = new ProviderFactory(this.getFhirContext(), this.registry, localSystemTerminologyProvider);

        resolveProviders(systemDao, providerFactory);

        setResourceProviders(registry.getResourceProviders().toArray(IResourceProvider[]::new));
        

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
         * This configures the server to page search results to and from
         * the database, instead of only paging them to memory. This may mean
         * a performance hit when performing searches that return lots of results,
         * but makes the server much more scalable.
         */
        setPagingProvider(appCtx.getBean(DatabaseBackedPagingProvider.class));

        /*
         * This interceptor formats the output using nice colourful
         * HTML output when the request is detected to come from a
         * browser.
         */
        ResponseHighlighterInterceptor responseHighlighterInterceptor = appCtx.getBean(ResponseHighlighterInterceptor.class);
        this.registerInterceptor(responseHighlighterInterceptor);

        /*
         * If you are hosting this server at a specific DNS name, the server will try to
         * figure out the FHIR base URL based on what the web container tells it, but
         * this doesn't always work. If you are setting links in your search bundles that
         * just refer to "localhost", you might want to use a server address strategy:
         */
        String serverAddress = HapiProperties.getServerAddress();
        if (serverAddress != null && serverAddress.length() > 0)
        {
            setServerAddressStrategy(new HardcodedServerAddressStrategy(serverAddress));
        }

        registerProvider(appCtx.getBean(TerminologyUploaderProviderDstu3.class));

        if (HapiProperties.getCorsEnabled())
        {
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

    // Since resource provider resolution not lazy, the providers here must be resolved in the correct
    // order of dependencies.
    private void resolveProviders(IFhirSystemDao systemDao, ProviderFactory providerFactory)
            throws ServletException
    {
        NarrativeProvider narrativeProvider = new NarrativeProvider();
        HQMFProvider hqmfProvider = new HQMFProvider();

        // ValueSet processing
        FHIRValueSetResourceProvider valueSetProvider =
                new FHIRValueSetResourceProvider(
                        (CodeSystemResourceProvider) this.registry.resolve("CodeSystem")
                );
        this.registerCustomResourceProvider(valueSetProvider);

        // Code System Update
        CodeSystemUpdateProvider csUpdate = new CodeSystemUpdateProvider((FHIRValueSetResourceProvider) this.registry.resolve("ValueSet"));
        this.registerProvider(csUpdate);

        //Library processing
        NarrativeLibraryResourceProvider libraryProvider = new NarrativeLibraryResourceProvider(narrativeProvider);
        this.registerCustomResourceProvider(libraryProvider);

        // CQL Execution
        CqlExecutionProvider cql = new CqlExecutionProvider(libraryProvider, providerFactory);
        this.registerProvider(cql);

        // Bundle processing
        FHIRBundleResourceProvider bundleProvider = new FHIRBundleResourceProvider(providerFactory);
        this.registerCustomResourceProvider(bundleProvider);

        // Endpoint processing
        FHIREndpointProvider endpointProvider = new FHIREndpointProvider(systemDao);
        this.registerCustomResourceProvider(endpointProvider);

        // Measure processing
        FHIRMeasureResourceProvider measureProvider = new FHIRMeasureResourceProvider(registry, providerFactory, systemDao, narrativeProvider, hqmfProvider);
        this.registerCustomResourceProvider(measureProvider);

        // // ActivityDefinition processing
        FHIRActivityDefinitionResourceProvider actDefProvider = new FHIRActivityDefinitionResourceProvider(this.fhirContext, cql);
        this.registerCustomResourceProvider(actDefProvider);

        // PlanDefinition processing
        FHIRPlanDefinitionResourceProvider planDefProvider = new FHIRPlanDefinitionResourceProvider(this.fhirContext, actDefProvider, libraryProvider);
        this.registerCustomResourceProvider(planDefProvider);
    }

    private <T extends IBaseResource> void registerCustomResourceProvider(BaseJpaResourceProvider<T>  provider) {

        BaseJpaResourceProvider<? extends IBaseResource> oldProvider = registry.resolve(provider.getResourceType().getSimpleName());
        IFhirResourceDao<T> oldDao = ((BaseJpaResourceProvider<T>)oldProvider).getDao();
        provider.setDao(oldDao);
        provider.setContext(oldProvider.getContext());

        this.registry.register(provider);
    }
}
