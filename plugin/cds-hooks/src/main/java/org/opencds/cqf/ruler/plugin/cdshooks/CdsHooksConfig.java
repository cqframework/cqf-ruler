package org.opencds.cqf.ruler.plugin.cdshooks;

import ca.uhn.fhir.jpa.starter.FhirTesterConfig;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.engine.model.CachingModelResolverDecorator;
import org.opencds.cqf.ruler.api.MetadataExtender;
import org.opencds.cqf.ruler.plugin.cdshooks.CdsHooksProperties;
import org.opencds.cqf.ruler.plugin.cdshooks.providers.ProviderConfiguration;
import org.opencds.cqf.ruler.plugin.cdshooks.r4.CdsHooksServlet;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.jpa.starter.annotations.OnDSTU3Condition;
import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

@Configuration
//@ConditionalOnProperty(prefix = "hapi.fhir.cdshooks", name = "enabledTest", havingValue = "true")
//@ComponentScan(basePackageClasses = { org.opencds.cqf.ruler.plugin.cdshooks.r4.CdsHooksServlet.class })
public class CdsHooksConfig {

	@Bean(name = "setHookProperties")
	@Primary
	@Conditional(OnR4Condition.class)
	public CdsHooksProperties HooksProperties() {
		return new CdsHooksProperties();
	}

	@Bean(name = "r4ModelResolver")
	@Primary
	@Conditional(OnR4Condition.class)
	public ModelResolver modelResolver() {
		return new CachingModelResolverDecorator(new R4FhirModelResolver());
	}

	@Bean
	@Primary
	@Conditional(OnR4Condition.class)
	public ProviderConfiguration providerConfiguration() { return ProviderConfiguration.DEFAULT_PROVIDER_CONFIGURATION; }

	@Bean
	@Conditional(OnR4Condition.class)
	public ServletRegistrationBean<CdsHooksServlet> cdsHooksRegistrationBean() {

		AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = new AnnotationConfigWebApplicationContext();
		annotationConfigWebApplicationContext.register(CdsHooksConfig.class);

		CdsHooksServlet cdsHooksServlet = new CdsHooksServlet();

		ServletRegistrationBean<CdsHooksServlet> registrationBean = new ServletRegistrationBean<CdsHooksServlet>();
		registrationBean.setServlet(cdsHooksServlet);
		registrationBean.addUrlMappings("/cds-services/*");
		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}
}

