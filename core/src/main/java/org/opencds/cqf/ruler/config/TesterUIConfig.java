package org.opencds.cqf.ruler.config;

import org.opencds.cqf.ruler.external.FhirTesterConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir", name = "tester_enabled", havingValue = "true", matchIfMissing = false)
@Import(FhirTesterConfig.class)
public class TesterUIConfig {
	@Bean
	public ServletRegistrationBean<DispatcherServlet> overlayRegistrationBean() {

		AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = new AnnotationConfigWebApplicationContext();
		annotationConfigWebApplicationContext.register(FhirTesterConfig.class);

		DispatcherServlet dispatcherServlet = new DispatcherServlet(annotationConfigWebApplicationContext);
		dispatcherServlet.setContextClass(AnnotationConfigWebApplicationContext.class);
		dispatcherServlet.setContextConfigLocation(FhirTesterConfig.class.getName());

		ServletRegistrationBean<DispatcherServlet> registrationBean = new ServletRegistrationBean<>();
		registrationBean.setServlet(dispatcherServlet);
		registrationBean.addUrlMappings("/*");
		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}
}
