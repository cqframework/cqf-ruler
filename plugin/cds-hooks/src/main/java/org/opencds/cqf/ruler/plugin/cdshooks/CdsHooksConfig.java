package org.opencds.cqf.ruler.plugin.cdshooks;

import ca.uhn.fhir.jpa.starter.FhirTesterConfig;
import org.opencds.cqf.ruler.plugin.cdshooks.CdsHooksProperties;
import org.opencds.cqf.ruler.plugin.cdshooks.r4.CdsHooksServlet;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.jpa.starter.annotations.OnDSTU3Condition;
import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

@Configuration
public class CdsHooksConfig {

	@Bean
	public CdsHooksProperties CdsHooksProperties() {
		return new CdsHooksProperties();
	}

	@Bean
	public SecurityProperties wqqeqweqweProperties() {
		return new SecurityProperties();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public ServletRegistrationBean<CdsHooksServlet> cdsHooksRegistrationBean() {
		CdsHooksServlet cdsHooksServlet = new CdsHooksServlet();
		ServletRegistrationBean<CdsHooksServlet> registrationBean = new ServletRegistrationBean<CdsHooksServlet>();
		registrationBean.setServlet(cdsHooksServlet);
		registrationBean.addUrlMappings("/cds-services/*");
		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}
}
