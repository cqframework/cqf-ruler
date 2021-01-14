package org.opencds.cqf.r4.servlet;

import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.opencds.cqf.common.config.HapiProperties;
import org.opencds.cqf.r4.providers.CqfRulerJpaConformanceProviderR4;
import org.opencds.cqf.r4.providers.OAuthProvider;
import org.opencds.cqf.tooling.measure.r4.CodeTerminologyRef;
import org.opencds.cqf.tooling.measure.r4.CqfMeasure;
import org.opencds.cqf.tooling.measure.r4.PopulationCriteriaMap;
import org.opencds.cqf.tooling.measure.r4.VersionedTerminologyRef;
import org.springframework.context.ApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.api.rp.ResourceProviderFactory;
import ca.uhn.fhir.jpa.provider.TerminologyUploaderProvider;
import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.searchparam.registry.ISearchParamRegistry;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
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

        DaoConfig daoConfig = appCtx.getBean(DaoConfig.class);
		daoConfig.setResourceClientIdStrategy(DaoConfig.ClientIdStrategyEnum.ANY);
        ISearchParamRegistry searchParamRegistry = appCtx.getBean(ISearchParamRegistry.class);

        // System and Resource Providers
        Object systemProvider = appCtx.getBean("mySystemProviderR4", JpaSystemProviderR4.class);
        registerProvider(systemProvider);


        ResourceProviderFactory resourceProviders = appCtx.getBean("myResourceProvidersR4", ResourceProviderFactory.class);
        registerProviders(resourceProviders.createProviders());

        List<Class<?>> operationsProviders = appCtx.getBean("myOperationProvidersR4", List.class);
        operationsProviders.forEach(x -> registerProvider(appCtx.getBean(x)));

        if(HapiProperties.getOAuthEnabled()) {
                OAuthProvider oauthProvider = new OAuthProvider();
                oauthProvider.setDaoConfig(daoConfig);
                oauthProvider.setSystemDao(systemDao);
                oauthProvider.setSearchParamRegistry(searchParamRegistry);
                oauthProvider.setImplementationDescription("CQF Ruler FHIR R4 Server");
                this.setServerConformanceProvider(oauthProvider);
            }else {
        
                CqfRulerJpaConformanceProviderR4 confProvider = new CqfRulerJpaConformanceProviderR4();
                confProvider.setDaoConfig(daoConfig);
                confProvider.setSystemDao(systemDao);
                confProvider.setSearchParamRegistry(searchParamRegistry);
                confProvider.setImplementationDescription("CQF Ruler FHIR R4 Server");
                this.setServerConformanceProvider(confProvider);
        }

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
}
