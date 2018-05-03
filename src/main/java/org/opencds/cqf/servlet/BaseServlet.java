package org.opencds.cqf.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.JpaSystemProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.*;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.providers.*;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletException;
import java.util.Collection;
import java.util.List;

/**
 * Created by Chris Schuler on 12/11/2016.
 */
public class BaseServlet extends RestfulServer {

    @SuppressWarnings("unchecked")
    @Override
    protected void initialize() throws ServletException {

        super.initialize();

        FhirVersionEnum fhirVersion = FhirVersionEnum.DSTU3;
        setFhirContext(new FhirContext(fhirVersion));

        // Get the spring context from the web container (it's declared in web.xml)
        WebApplicationContext myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();

        String resourceProviderBeanName = "myResourceProvidersDstu3";
        List<IResourceProvider> beans = myAppCtx.getBean(resourceProviderBeanName, List.class);
        setResourceProviders(beans);

        Object systemProvider = myAppCtx.getBean("mySystemProviderDstu3", JpaSystemProviderDstu3.class);
        setPlainProviders(systemProvider);

        IFhirSystemDao<Bundle, Meta> systemDao = myAppCtx.getBean("mySystemDaoDstu3", IFhirSystemDao.class);
        JpaConformanceProviderDstu3 confProvider = new JpaConformanceProviderDstu3(this, systemDao,
                myAppCtx.getBean(DaoConfig.class));
        confProvider.setImplementationDescription("Measure and Opioid Processing Server");
        setServerConformanceProvider(confProvider);

        FhirContext ctx = getFhirContext();
        ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
        setDefaultPrettyPrint(true);
        setDefaultResponseEncoding(EncodingEnum.JSON);
        setPagingProvider(myAppCtx.getBean(DatabaseBackedPagingProvider.class));

        /*
		 * Enable CORS
		 */
//        CorsConfiguration config = new CorsConfiguration();
//        CorsInterceptor corsInterceptor = new CorsInterceptor(config);
//        config.addAllowedHeader("Origin");
//        config.addAllowedHeader("Accept");
//        config.addAllowedHeader("X-Requested-With");
//        config.addAllowedHeader("Content-Type");
//        config.addAllowedHeader("Access-Control-Request-Method");
//        config.addAllowedHeader("Access-Control-Request-Headers");
//        config.addAllowedOrigin("*");
//        config.addExposedHeader("Location");
//        config.addExposedHeader("Content-Location");
//        config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
//        registerInterceptor(corsInterceptor);

        /*
		 * Load interceptors for the server from Spring (these are defined in FhirServerConfig.java)
		 */
        Collection<IServerInterceptor> interceptorBeans = myAppCtx.getBeansOfType(IServerInterceptor.class).values();
        for (IServerInterceptor interceptor : interceptorBeans) {
            this.registerInterceptor(interceptor);
        }

        JpaDataProvider provider = new JpaDataProvider(getResourceProviders());
        JpaResourceProviderDstu3<ValueSet> vs = (ValueSetResourceProvider)   provider.resolveResourceProvider("ValueSet");
        JpaResourceProviderDstu3<CodeSystem> cs = (CodeSystemResourceProvider) provider.resolveResourceProvider("CodeSystem");
        TerminologyProvider terminologyProvider = new JpaTerminologyProvider(vs, cs);
        provider.setTerminologyProvider(terminologyProvider);

        // Bundle processing
        FHIRBundleResourceProvider bundleProvider = new FHIRBundleResourceProvider();
        BundleResourceProvider jpaBundleProvider = (BundleResourceProvider) getProvider("Bundle");
        bundleProvider.setDao(jpaBundleProvider.getDao());
        bundleProvider.setContext(jpaBundleProvider.getContext());

        try {
            unregisterProvider(jpaBundleProvider);
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        registerProvider(bundleProvider);

        // Measure processing
        FHIRMeasureResourceProvider measureProvider = new FHIRMeasureResourceProvider(provider);
        MeasureResourceProvider jpaMeasureProvider = (MeasureResourceProvider) getProvider("Measure");
        measureProvider.setDao(jpaMeasureProvider.getDao());
        measureProvider.setContext(jpaMeasureProvider.getContext());

        try {
            unregisterProvider(jpaMeasureProvider);
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        registerProvider(measureProvider);

        // ActivityDefinition processing
        FHIRActivityDefinitionResourceProvider actDefProvider = new FHIRActivityDefinitionResourceProvider(getResourceProviders());
        ActivityDefinitionResourceProvider jpaActDefProvider = (ActivityDefinitionResourceProvider) getProvider("ActivityDefinition");
        actDefProvider.setDao(jpaActDefProvider.getDao());
        actDefProvider.setContext(jpaActDefProvider.getContext());

        try {
            unregisterProvider(jpaActDefProvider);
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        registerProvider(actDefProvider);

        // PlanDefinition processing
        FHIRPlanDefinitionResourceProvider planDefProvider = new FHIRPlanDefinitionResourceProvider(getResourceProviders());
        PlanDefinitionResourceProvider jpaPlanDefProvider = (PlanDefinitionResourceProvider) getProvider("PlanDefinition");
        planDefProvider.setDao(jpaPlanDefProvider.getDao());
        planDefProvider.setContext(jpaPlanDefProvider.getContext());

        try {
            unregisterProvider(jpaPlanDefProvider);
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        registerProvider(planDefProvider);

        // StructureMap processing
        FHIRStructureMapResourceProvider structureMapProvider = new FHIRStructureMapResourceProvider(getResourceProviders());
        StructureMapResourceProvider jpaStructMapProvider = (StructureMapResourceProvider) getProvider("StructureMap");
        structureMapProvider.setDao(jpaStructMapProvider.getDao());
        structureMapProvider.setContext(jpaStructMapProvider.getContext());

        try {
            unregisterProvider(jpaStructMapProvider);
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        registerProvider(structureMapProvider);

        // Patient processing - for bulk data export
        BulkDataPatientProvider bulkDataPatientProvider = new BulkDataPatientProvider(getResourceProviders());
        PatientResourceProvider jpaPatientProvider = (PatientResourceProvider) getProvider("Patient");
        bulkDataPatientProvider.setDao(jpaPatientProvider.getDao());
        bulkDataPatientProvider.setContext(jpaPatientProvider.getContext());

        try {
            unregisterProvider(jpaPatientProvider);
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        registerProvider(bulkDataPatientProvider);

        // Group processing - for bulk data export
        BulkDataGroupProvider bulkDataGroupProvider = new BulkDataGroupProvider(getResourceProviders());
        GroupResourceProvider jpaGroupProvider = (GroupResourceProvider) getProvider("Group");
        bulkDataGroupProvider.setDao(jpaGroupProvider.getDao());
        bulkDataGroupProvider.setContext(jpaGroupProvider.getContext());

        try {
            unregisterProvider(jpaGroupProvider);
        } catch (Exception e) {
            throw new ServletException("Unable to unregister provider: " + e.getMessage());
        }

        registerProvider(bulkDataGroupProvider);

        // Register the logging interceptor
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        this.registerInterceptor(loggingInterceptor);

        // The SLF4j logger "test.accesslog" will receive the logging events
        loggingInterceptor.setLoggerName("logging.accesslog");

        // This is the format for each line. A number of substitution variables may
        // be used here. See the JavaDoc for LoggingInterceptor for information on
        // what is available.
        loggingInterceptor.setMessageFormat("Source[${remoteAddr}] Operation[${operationType} ${idOrResourceName}] UA[${requestHeader.user-agent}] Params[${requestParameters}]");

        //setServerAddressStrategy(new HardcodedServerAddressStrategy("http://mydomain.com/fhir/baseDstu2"));
        //registerProvider(myAppCtx.getBean(TerminologyUploaderProviderDstu3.class));
    }

    public IResourceProvider getProvider(String name) {

        for (IResourceProvider res : getResourceProviders()) {
            if (res.getResourceType().getSimpleName().equals(name)) {
                return res;
            }
        }

        throw new IllegalArgumentException("This should never happen!");
    }
}
