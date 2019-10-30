package org.opencds.cqf.dstu3.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.JpaSystemProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.TerminologyUploaderProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.*;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.term.IHapiTerminologySvcDstu3;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Meta;
import org.opencds.cqf.config.HapiProperties;
import org.opencds.cqf.config.ResourceProviderRegistry;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.dstu3.providers.*;
import org.opencds.cqf.dstu3.providers.LibraryResourceProvider;
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

public class BaseServlet extends RestfulServer
{
    @SuppressWarnings("unchecked")
    @Override
    protected void initialize() throws ServletException
    {
        // AnnotationConfigWebApplicationContext servletCtx = (AnnotationConfigWebApplicationContext)this.getServletContext();
        // servletCtx.register(
        //     FhirServerConfigDstu3.class, 
        //     FhirServerConfigCommon.class);

        super.initialize();

        // System level providers
        ApplicationContext appCtx = (ApplicationContext) getServletContext().getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");
        setFhirContext(appCtx.getBean(FhirContext.class));

        Object systemProvider = appCtx.getBean("mySystemProviderDstu3", JpaSystemProviderDstu3.class);
        registerProvider(systemProvider);

        IFhirSystemDao<Bundle, Meta> systemDao = appCtx.getBean("mySystemDaoDstu3", IFhirSystemDao.class);
        JpaConformanceProviderDstu3 confProvider = new JpaConformanceProviderDstu3(this, systemDao, appCtx.getBean(DaoConfig.class));
        confProvider.setImplementationDescription("CQF Ruler FHIR DSTU3 Server");
        setServerConformanceProvider(confProvider);

        // Resource Providers
        // This registry holds a collection of all the resource providers on this server.
        // This is essentially a duplicate of the resourceProviders on the underlying RestfulServer
        // with the intention that both collections will be synchronized at the end of the initialization
        // and the registry can be used in various classes that needs access the to resource providers.
        List<IResourceProvider> resourceProviders = appCtx.getBean("myResourceProvidersDstu3", List.class);
        ResourceProviderRegistry registry = new ResourceProviderRegistry();
        for (IResourceProvider provider : resourceProviders) {
            registry.register(provider);
        }


        List<Object> plainProviders = new ArrayList<>();
        plainProviders.add(appCtx.getBean(TerminologyUploaderProviderDstu3.class));

        TerminologyProvider localSystemTerminologyProvider = new JpaTerminologyProvider(appCtx.getBean("terminologyService", IHapiTerminologySvcDstu3.class), getFhirContext(), (ValueSetResourceProvider) registry.resolve("ValueSet"));
        ProviderFactory factory = new ProviderFactory(this.getFhirContext(), registry, localSystemTerminologyProvider);

        resolveResourceProviders(registry, systemDao);

        CqlExecutionProvider cql = new CqlExecutionProvider((LibraryResourceProvider)registry.resolve("Library"), factory);
        plainProviders.add(cql);


        // CodeSystemUpdateProvider csUpdate = new CodeSystemUpdateProvider(provider);
        // plainProviders.add(csUpdate);

        setResourceProviders(registry.getResourceProviders().toArray(IResourceProvider[]::new));
        registerProviders(plainProviders);
        
  

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

    private void resolveResourceProviders(ResourceProviderRegistry registry, IFhirSystemDao systemDao)
            throws ServletException
    {
        NarrativeProvider narrativeProvider = new NarrativeProvider();
        HQMFProvider hqmfProvider = new HQMFProvider();

        // ValueSet processing
        FHIRValueSetResourceProvider valueSetProvider =
                new FHIRValueSetResourceProvider(
                        (CodeSystemResourceProvider) registry.resolve("CodeSystem")
                );
        ValueSetResourceProvider jpaValueSetProvider = (ValueSetResourceProvider) registry.resolve("ValueSet");
        valueSetProvider.setDao(jpaValueSetProvider.getDao());
        valueSetProvider.setContext(jpaValueSetProvider.getContext());

        registry.register(valueSetProvider);

        //Library processing
        NarrativeLibraryResourceProvider libraryProvider = new NarrativeLibraryResourceProvider(narrativeProvider);
        ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider jpaLibraryProvider = 
            (ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider) registry.resolve("Library");
        libraryProvider.setDao(jpaLibraryProvider.getDao());
        libraryProvider.setContext(jpaLibraryProvider.getContext());

        registry.register(libraryProvider);


        // Bundle processing
        // FHIRBundleResourceProvider bundleProvider = new FHIRBundleResourceProvider(registry);
        // BundleResourceProvider jpaBundleProvider = (BundleResourceProvider) registry.resolve("Bundle");
        // bundleProvider.setDao(jpaBundleProvider.getDao());
        // bundleProvider.setContext(jpaBundleProvider.getContext());

        // try {
        //     unregister(jpaBundleProvider, registry.getCollectionProviders());
        // } catch (Exception e) {
        //     throw new ServletException("Unable to unregister provider: " + e.getMessage());
        // }

        // register(bundleProvider, registry.getCollectionProviders());



        // // Measure processing
        // FHIRMeasureResourceProvider measureProvider = new FHIRMeasureResourceProvider(registry, systemDao, narrativeProvider, hqmfProvider);
        // MeasureResourceProvider jpaMeasureProvider = (MeasureResourceProvider) registry.resolve("Measure");
        // measureProvider.setDao(jpaMeasureProvider.getDao());
        // measureProvider.setContext(jpaMeasureProvider.getContext());

        // try {
        //     unregister(jpaMeasureProvider, registry.getCollectionProviders());
        // } catch (Exception e) {
        //     throw new ServletException("Unable to unregister provider: " + e.getMessage());
        // }

        // register(measureProvider, registry.getCollectionProviders());

        // // ActivityDefinition processing
        // FHIRActivityDefinitionResourceProvider actDefProvider = new FHIRActivityDefinitionResourceProvider(registry);
        // ActivityDefinitionResourceProvider jpaActDefProvider = (ActivityDefinitionResourceProvider) registry.resolve("ActivityDefinition");
        // actDefProvider.setDao(jpaActDefProvider.getDao());
        // actDefProvider.setContext(jpaActDefProvider.getContext());

        // try {
        //     unregister(jpaActDefProvider, registry.getCollectionProviders());
        // } catch (Exception e) {
        //     throw new ServletException("Unable to unregister provider: " + e.getMessage());
        // }

        // register(actDefProvider, registry.getCollectionProviders());

        // // PlanDefinition processing
        // FHIRPlanDefinitionResourceProvider planDefProvider = new FHIRPlanDefinitionResourceProvider(registry);
        // PlanDefinitionResourceProvider jpaPlanDefProvider = (PlanDefinitionResourceProvider) registry.resolve("PlanDefinition");
        // planDefProvider.setDao(jpaPlanDefProvider.getDao());
        // planDefProvider.setContext(jpaPlanDefProvider.getContext());

        // try {
        //     unregister(jpaPlanDefProvider, registry.getCollectionProviders());
        // } catch (Exception e) {
        //     throw new ServletException("Unable to unregister provider: " + e.getMessage());
        // }

        // register(planDefProvider, registry.getCollectionProviders());

        // // Endpoint processing
        // FHIREndpointProvider endpointProvider = new FHIREndpointProvider(registry, systemDao);
        // EndpointResourceProvider jpaEndpointProvider = (EndpointResourceProvider) registry.resolve("Endpoint");
        // endpointProvider.setDao(jpaEndpointProvider.getDao());
        // endpointProvider.setContext(jpaEndpointProvider.getContext());

        // try {
        //     unregister(jpaEndpointProvider, registry.getCollectionProviders());
        // } catch (Exception e) {
        //     throw new ServletException("Unable to unregister provider: " + e.getMessage());
        // }

        // register(endpointProvider, registry.getCollectionProviders());
    }
}
