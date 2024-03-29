package org.opencds.cqf.ruler.cdshooks;

import org.opencds.cqf.external.annotations.OnR4Condition;
import org.opencds.cqf.ruler.cdshooks.providers.ProviderConfiguration;
import org.opencds.cqf.ruler.cdshooks.r4.CdsHooksServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.cache.IResourceChangeListenerRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.cdshooks", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CdsHooksConfig {

	@Autowired
	AutowireCapableBeanFactory beanFactory;

	@Bean
	public CdsHooksProperties cdsHooksProperties() {
		return new CdsHooksProperties();
	}

	@Bean
	public ProviderConfiguration providerConfiguration(CdsHooksProperties cdsProperties) {
		return new ProviderConfiguration(cdsProperties);
	}

	@Bean
	public CdsServicesCache cdsServiceInterceptor(IResourceChangeListenerRegistry resourceChangeListenerRegistry,
			DaoRegistry daoRegistry) {
		CdsServicesCache listener = new CdsServicesCache(daoRegistry);
		resourceChangeListenerRegistry.registerResourceResourceChangeListener("PlanDefinition",
				SearchParameterMap.newSynchronous(), listener, 1000);
		return listener;
	}
	/*
	 * @Bean
	 * 
	 * @Conditional(OnDSTU3Condition.class)
	 * 
	 * @DependsOn({ "dstu3CqlExecutionProvider", "dstu3LibraryEvaluationProvider" })
	 * public
	 * ServletRegistrationBean<org.opencds.cqf.ruler.cdshooks.dstu3.CdsHooksServlet>
	 * cdsHooksRegistrationBeanDstu3() {
	 * org.opencds.cqf.ruler.cdshooks.dstu3.CdsHooksServlet cdsHooksServlet = new
	 * org.opencds.cqf.ruler.cdshooks.dstu3.CdsHooksServlet();
	 * beanFactory.autowireBean(cdsHooksServlet);
	 * 
	 * ServletRegistrationBean<org.opencds.cqf.ruler.cdshooks.dstu3.CdsHooksServlet>
	 * registrationBean = new ServletRegistrationBean<>();
	 * registrationBean.setName("cds-hooks servlet");
	 * registrationBean.setServlet(cdsHooksServlet);
	 * registrationBean.addUrlMappings("/cds-services/*");
	 * registrationBean.setLoadOnStartup(1);
	 * return registrationBean;
	 * }
	 */

	@Bean
	@Conditional(OnR4Condition.class)
	public ServletRegistrationBean<org.opencds.cqf.ruler.cdshooks.r4.CdsHooksServlet> cdsHooksRegistrationBeanR4() {
		org.opencds.cqf.ruler.cdshooks.r4.CdsHooksServlet cdsHooksServlet = new org.opencds.cqf.ruler.cdshooks.r4.CdsHooksServlet();
		beanFactory.autowireBean(cdsHooksServlet);

		ServletRegistrationBean<CdsHooksServlet> registrationBean = new ServletRegistrationBean<>();
		registrationBean.setName("cds-hooks servlet");
		registrationBean.setServlet(cdsHooksServlet);
		registrationBean.addUrlMappings("/cds-services/*");
		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}
}
