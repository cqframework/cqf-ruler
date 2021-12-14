package org.opencds.cqf.ruler.plugin.cdshooks;

import org.opencds.cqf.ruler.plugin.cdshooks.providers.ProviderConfiguration;
import org.opencds.cqf.ruler.plugin.cdshooks.r4.CdsHooksServlet;
import org.opencds.cqf.ruler.plugin.cql.CqlConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.cdshooks", name = "enabled", havingValue = "true")
public class CdsHooksConfig {

	@Bean(name = "setHookProperties")
	public CdsHooksProperties cdsHooksProperties() {
		return new CdsHooksProperties();
	}

//	@Bean(name = "r4ModelResolver")
//	@Primary
//	@Conditional(OnR4Condition.class)
//	public ModelResolver modelResolver() {
//		return new CachingModelResolverDecorator(new R4FhirModelResolver());
//	}

	@Bean
	public CqlConfig cqlConfiguration() { return new CqlConfig(); }

	@Bean
	public SecurityProperties securityProperties() { return new SecurityProperties(); }

//	@Bean
//	@Primary
//	@Conditional(OnR4Condition.class)
//	public LibraryHelper libraryHelper(Map<VersionedIdentifier, Model> modelCache, Map<org.cqframework.cql.elm.execution.VersionedIdentifier,
//		org.cqframework.cql.elm.execution.Library> libracheCache, CqlTranslatorOptions translatorOptions) {
//		return new LibraryHelper(modelCache, libracheCache, translatorOptions); }

	@Bean
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

