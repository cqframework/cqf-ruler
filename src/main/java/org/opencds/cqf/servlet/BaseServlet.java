package org.opencds.cqf.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.config.WebsocketDispatcherConfig;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.dao.dstu3.FhirSystemDaoDstu3;
import ca.uhn.fhir.jpa.provider.JpaConformanceProviderDstu2;
import ca.uhn.fhir.jpa.provider.JpaSystemProviderDstu2;
import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.JpaSystemProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.TerminologyUploaderProviderDstu3;
import ca.uhn.fhir.jpa.provider.r4.JpaConformanceProviderR4;
import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.jpa.provider.r4.TerminologyUploaderProviderR4;
import ca.uhn.fhir.jpa.rp.dstu3.*;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.term.IHapiTerminologySvcDstu3;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.*;
import org.apache.commons.lang3.StringUtils;
import org.opencds.cqf.config.FhirServerConfigDstu2;
import org.opencds.cqf.config.FhirServerConfigDstu3;
import org.opencds.cqf.config.FhirServerConfigR4;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.interceptors.TransactionInterceptor;
import org.opencds.cqf.providers.*;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by Chris Schuler on 12/11/2016.
 */
public class BaseServlet extends RestfulServer {

    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(BaseServlet.class);

    private JpaDataProvider provider;
    public JpaDataProvider getProvider() {
        return provider;
    }

    private AnnotationConfigWebApplicationContext myAppCtx;

    private static final String FHIR_BASEURL_DSTU2 = "fhir.baseurl.dstu2";
    private static final String FHIR_BASEURL_DSTU3 = "fhir.baseurl.dstu3";
    private static final String FHIR_BASEURL_R4 = "fhir.baseurl.r4";

    @SuppressWarnings("unchecked")
    @Override
    protected void initialize() throws ServletException {

        super.initialize();

        WebApplicationContext parentAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();

        String implDesc = getInitParameter("ImplementationDescription");
        String fhirVersionParam = getInitParameter("FhirVersion");
        if (StringUtils.isBlank(fhirVersionParam)) {
            fhirVersionParam = "DSTU3";
        }
        fhirVersionParam = fhirVersionParam.trim().toUpperCase();

        List<IResourceProvider> beans;
        @SuppressWarnings("rawtypes")
        IFhirSystemDao systemDao;
        ETagSupportEnum etagSupport;
        String baseUrlProperty;
        List<Object> plainProviders = new ArrayList<>();

        switch (fhirVersionParam) {
            case "DSTU2": {
                myAppCtx = new AnnotationConfigWebApplicationContext();
                myAppCtx.setServletConfig(getServletConfig());
                myAppCtx.setParent(parentAppCtx);
                myAppCtx.register(FhirServerConfigDstu2.class, WebsocketDispatcherConfig.class);
                baseUrlProperty = FHIR_BASEURL_DSTU2;
                myAppCtx.refresh();
                setFhirContext(FhirContext.forDstu2());
                beans = myAppCtx.getBean("myResourceProvidersDstu2", List.class);
                plainProviders.add(myAppCtx.getBean("mySystemProviderDstu2", JpaSystemProviderDstu2.class));
                systemDao = myAppCtx.getBean("mySystemDaoDstu2", IFhirSystemDao.class);
                etagSupport = ETagSupportEnum.ENABLED;
                JpaConformanceProviderDstu2 confProvider = new JpaConformanceProviderDstu2(this, systemDao, myAppCtx.getBean(DaoConfig.class));
                confProvider.setImplementationDescription(implDesc);
                setServerConformanceProvider(confProvider);
                break;
            }
            case "DSTU3": {
                myAppCtx = new AnnotationConfigWebApplicationContext();
                myAppCtx.setServletConfig(getServletConfig());
                myAppCtx.setParent(parentAppCtx);
                myAppCtx.register(FhirServerConfigDstu3.class, WebsocketDispatcherConfig.class);
                baseUrlProperty = FHIR_BASEURL_DSTU3;
                myAppCtx.refresh();
                setFhirContext(FhirContext.forDstu3());
                beans = myAppCtx.getBean("myResourceProvidersDstu3", List.class);
                plainProviders.add(myAppCtx.getBean("mySystemProviderDstu3", JpaSystemProviderDstu3.class));
                systemDao = myAppCtx.getBean("mySystemDaoDstu3", IFhirSystemDao.class);
                etagSupport = ETagSupportEnum.ENABLED;
                JpaConformanceProviderDstu3 confProvider = new JpaConformanceProviderDstu3(this, systemDao, myAppCtx.getBean(DaoConfig.class));
                confProvider.setImplementationDescription(implDesc);
                setServerConformanceProvider(confProvider);
                plainProviders.add(myAppCtx.getBean(TerminologyUploaderProviderDstu3.class));
                provider = new JpaDataProvider(beans);
                TerminologyProvider terminologyProvider = new JpaTerminologyProvider(myAppCtx.getBean("terminologyService", IHapiTerminologySvcDstu3.class), getFhirContext(), (ValueSetResourceProvider) provider.resolveResourceProvider("ValueSet"));
                provider.setTerminologyProvider(terminologyProvider);
                resolveResourceProviders(provider, systemDao);
                break;
            }
            case "R4": {
                myAppCtx = new AnnotationConfigWebApplicationContext();
                myAppCtx.setServletConfig(getServletConfig());
                myAppCtx.setParent(parentAppCtx);
                myAppCtx.register(FhirServerConfigR4.class, WebsocketDispatcherConfig.class);
                baseUrlProperty = FHIR_BASEURL_R4;
                myAppCtx.refresh();
                setFhirContext(FhirContext.forR4());
                beans = myAppCtx.getBean("myResourceProvidersR4", List.class);
                plainProviders.add(myAppCtx.getBean("mySystemProviderR4", JpaSystemProviderR4.class));
                systemDao = myAppCtx.getBean("mySystemDaoR4", IFhirSystemDao.class);
                etagSupport = ETagSupportEnum.ENABLED;
                JpaConformanceProviderR4 confProvider = new JpaConformanceProviderR4(this, systemDao, myAppCtx.getBean(DaoConfig.class));
                confProvider.setImplementationDescription(implDesc);
                setServerConformanceProvider(confProvider);
                plainProviders.add(myAppCtx.getBean(TerminologyUploaderProviderR4.class));
                break;
            }
            default:
                throw new ServletException("Unknown FHIR version specified in init-param[FhirVersion]: " + fhirVersionParam);
        }

        setETagSupport(etagSupport);

        FhirContext ctx = getFhirContext();
        ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());

        for (IResourceProvider nextResourceProvider : beans) {
            ourLog.info(" * Have resource provider for: {}", nextResourceProvider.getResourceType().getSimpleName());
        }
        setResourceProviders(beans);

        setPlainProviders(plainProviders);

        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedHeader("x-fhir-starter");
        config.addAllowedHeader("Origin");
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("X-Requested-With");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Cache-Control");

        config.addAllowedOrigin("*");

        config.addExposedHeader("Location");
        config.addExposedHeader("Content-Location");
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Create the interceptor and register it
        CorsInterceptor corsInterceptor = new CorsInterceptor(config);
        registerInterceptor(corsInterceptor);

        ResponseHighlighterInterceptor responseHighlighterInterceptor = new ResponseHighlighterInterceptor();
        responseHighlighterInterceptor.setShowRequestHeaders(false);
        responseHighlighterInterceptor.setShowResponseHeaders(true);
        registerInterceptor(responseHighlighterInterceptor);

        registerInterceptor(new BanUnsupportedHttpMethodsInterceptor());

        setDefaultPrettyPrint(true);
        setDefaultResponseEncoding(EncodingEnum.JSON);

        String baseUrl = System.getProperty(baseUrlProperty);
        if (StringUtils.isBlank(baseUrl)) {
            switch (fhirVersionParam) {
                case "R4":
                    baseUrl = "http://measure.eval.kanvix.com/cqf-ruler/baseR4";
                    break;
                case "DSTU3":
                    baseUrl = "http://measure.eval.kanvix.com/cqf-ruler/baseDstu3";
                    break;
                case "DSTU2":
                    baseUrl = "http://measure.eval.kanvix.com/cqf-ruler/baseDstu2";
                    break;
                default:
                    throw new ServletException("Unexpected fhir version encountered: " + fhirVersionParam);
            }
        }
        setServerAddressStrategy(new MyHardcodedServerAddressStrategy(baseUrl));

        setPagingProvider(myAppCtx.getBean(DatabaseBackedPagingProvider.class));

        Collection<IServerInterceptor> interceptorBeans = myAppCtx.getBeansOfType(IServerInterceptor.class).values();
        for (IServerInterceptor interceptor : interceptorBeans) {
            this.registerInterceptor(interceptor);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        ourLog.info("Server is shutting down");
        myAppCtx.close();
    }

    private void resolveResourceProviders(JpaDataProvider provider, IFhirSystemDao systemDao) throws ServletException {
        // Bundle processing
        FHIRBundleResourceProvider bundleProvider = new FHIRBundleResourceProvider(provider);
        BundleResourceProvider jpaBundleProvider = (BundleResourceProvider) provider.resolveResourceProvider("Bundle");
        bundleProvider.setDao(jpaBundleProvider.getDao());
        bundleProvider.setContext(jpaBundleProvider.getContext());

        try {
            unregister(jpaBundleProvider, provider.getCollectionProviders());
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        register(bundleProvider, provider.getCollectionProviders());

        // ValueSet processing
        FHIRValueSetResourceProvider valueSetProvider = new FHIRValueSetResourceProvider(provider);
        ValueSetResourceProvider jpaValueSetProvider = (ValueSetResourceProvider) provider.resolveResourceProvider("ValueSet");
        valueSetProvider.setDao(jpaValueSetProvider.getDao());
        valueSetProvider.setContext(jpaValueSetProvider.getContext());

        try {
            unregister(jpaValueSetProvider, provider.getCollectionProviders());
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        register(valueSetProvider, provider.getCollectionProviders());
        TransactionInterceptor transactionInterceptor = new TransactionInterceptor(valueSetProvider);
        registerInterceptor(transactionInterceptor);

        // Measure processing
        FHIRMeasureResourceProvider measureProvider = new FHIRMeasureResourceProvider(provider, systemDao);
        MeasureResourceProvider jpaMeasureProvider = (MeasureResourceProvider) provider.resolveResourceProvider("Measure");
        measureProvider.setDao(jpaMeasureProvider.getDao());
        measureProvider.setContext(jpaMeasureProvider.getContext());

        try {
            unregister(jpaMeasureProvider, provider.getCollectionProviders());
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        register(measureProvider, provider.getCollectionProviders());

        // ActivityDefinition processing
        FHIRActivityDefinitionResourceProvider actDefProvider = new FHIRActivityDefinitionResourceProvider(provider);
        ActivityDefinitionResourceProvider jpaActDefProvider = (ActivityDefinitionResourceProvider) provider.resolveResourceProvider("ActivityDefinition");
        actDefProvider.setDao(jpaActDefProvider.getDao());
        actDefProvider.setContext(jpaActDefProvider.getContext());

        try {
            unregister(jpaActDefProvider, provider.getCollectionProviders());
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        register(actDefProvider, provider.getCollectionProviders());

        // PlanDefinition processing
        FHIRPlanDefinitionResourceProvider planDefProvider = new FHIRPlanDefinitionResourceProvider(provider);
        PlanDefinitionResourceProvider jpaPlanDefProvider = (PlanDefinitionResourceProvider) provider.resolveResourceProvider("PlanDefinition");
        planDefProvider.setDao(jpaPlanDefProvider.getDao());
        planDefProvider.setContext(jpaPlanDefProvider.getContext());

        try {
            unregister(jpaPlanDefProvider, provider.getCollectionProviders());
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        register(planDefProvider, provider.getCollectionProviders());

        // StructureMap processing
        FHIRStructureMapResourceProvider structureMapProvider = new FHIRStructureMapResourceProvider(provider);
        StructureMapResourceProvider jpaStructMapProvider = (StructureMapResourceProvider) provider.resolveResourceProvider("StructureMap");
        structureMapProvider.setDao(jpaStructMapProvider.getDao());
        structureMapProvider.setContext(jpaStructMapProvider.getContext());

        try {
            unregister(jpaStructMapProvider, provider.getCollectionProviders());
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        register(structureMapProvider, provider.getCollectionProviders());

        // Patient processing - for bulk data export
        BulkDataPatientProvider bulkDataPatientProvider = new BulkDataPatientProvider(provider);
        PatientResourceProvider jpaPatientProvider = (PatientResourceProvider) provider.resolveResourceProvider("Patient");
        bulkDataPatientProvider.setDao(jpaPatientProvider.getDao());
        bulkDataPatientProvider.setContext(jpaPatientProvider.getContext());

        try {
            unregister(jpaPatientProvider, provider.getCollectionProviders());
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        register(bulkDataPatientProvider, provider.getCollectionProviders());

        // Group processing - for bulk data export
        BulkDataGroupProvider bulkDataGroupProvider = new BulkDataGroupProvider(provider);
        GroupResourceProvider jpaGroupProvider = (GroupResourceProvider) provider.resolveResourceProvider("Group");
        bulkDataGroupProvider.setDao(jpaGroupProvider.getDao());
        bulkDataGroupProvider.setContext(jpaGroupProvider.getContext());

        try {
            unregister(jpaGroupProvider, provider.getCollectionProviders());
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        register(bulkDataGroupProvider, provider.getCollectionProviders());
    }

    private void register(IResourceProvider provider, Collection<IResourceProvider> providers) {
        providers.add(provider);
    }

    private void unregister(IResourceProvider provider, Collection<IResourceProvider> providers) {
        providers.remove(provider);
    }

    public IResourceProvider getProvider(String name) {

        for (IResourceProvider res : getResourceProviders()) {
            if (res.getResourceType().getSimpleName().equals(name)) {
                return res;
            }
        }

        throw new IllegalArgumentException("This should never happen!");
    }

    private static class MyHardcodedServerAddressStrategy extends HardcodedServerAddressStrategy {

        MyHardcodedServerAddressStrategy(String theBaseUrl) {
            super(theBaseUrl);
        }

        @Override
        public String determineServerBase(ServletContext theServletContext, HttpServletRequest theRequest) {
			/*
			 * This is a bit of a hack, but we want to support both HTTP and HTTPS seamlessly
			 * so we have the outer http proxy relay requests to the Java container on
			 * port 28080 for http and 28081 for https.
			 */
            String retVal = super.determineServerBase(theServletContext, theRequest);
            if (theRequest.getRequestURL().indexOf("28081") != -1) {
                retVal = retVal.replace("http://", "https://");
            }
            return retVal;
        }

    }
}
