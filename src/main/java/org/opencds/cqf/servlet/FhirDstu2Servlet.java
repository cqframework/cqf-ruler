package org.opencds.cqf.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.JpaConformanceProviderDstu2;
import ca.uhn.fhir.jpa.provider.JpaSystemProviderDstu2;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.model.dstu2.composite.MetaDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.server.EncodingEnum;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

import javax.servlet.ServletException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by Christopher on 5/4/2017.
 */
public class FhirDstu2Servlet extends RestfulServer {

    @SuppressWarnings("unchecked")
    @Override
    protected void initialize() throws ServletException {
        super.initialize();

        FhirVersionEnum fhirVersion = FhirVersionEnum.DSTU2;
        setFhirContext(new FhirContext(fhirVersion));

        WebApplicationContext myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();

        String resourceProviderBeanName = "myResourceProvidersDstu2";
        List<IResourceProvider> beans = myAppCtx.getBean(resourceProviderBeanName, List.class);
        setResourceProviders(beans);

        Object systemProvider = myAppCtx.getBean("mySystemProviderDstu2", JpaSystemProviderDstu2.class);
        setPlainProviders(systemProvider);

        IFhirSystemDao<Bundle, MetaDt> systemDao = myAppCtx.getBean("mySystemDaoDstu2", IFhirSystemDao.class);
        JpaConformanceProviderDstu2 confProvider = new JpaConformanceProviderDstu2(this, systemDao,
                myAppCtx.getBean(DaoConfig.class));
        confProvider.setImplementationDescription("Example Server");
        setServerConformanceProvider(confProvider);

        FhirContext ctx = getFhirContext();
        ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());

        setDefaultPrettyPrint(true);
        setDefaultResponseEncoding(EncodingEnum.JSON);

        setPagingProvider(myAppCtx.getBean(DatabaseBackedPagingProvider.class));

		/*
		 * Enable CORS
		 */
        CorsConfiguration config = new CorsConfiguration();
        CorsInterceptor corsInterceptor = new CorsInterceptor(config);
        config.addAllowedHeader("Origin");
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("Prefer");
        config.addAllowedHeader("X-Requested-With");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("Access-Control-Request-Method");
        config.addAllowedHeader("Access-Control-Request-Headers");
        config.addAllowedOrigin("*");
        config.addExposedHeader("Location");
        config.addExposedHeader("Content-Location");
        config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
        registerInterceptor(corsInterceptor);

		/*
		 * Load interceptors for the server from Spring (these are defined in FhirServerConfig.java)
		 */
        Collection<IServerInterceptor> interceptorBeans = myAppCtx.getBeansOfType(IServerInterceptor.class).values();
        for (IServerInterceptor interceptor : interceptorBeans) {
            this.registerInterceptor(interceptor);
        }


    }
}
