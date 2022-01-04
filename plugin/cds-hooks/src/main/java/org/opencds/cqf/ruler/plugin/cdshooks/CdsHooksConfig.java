package org.opencds.cqf.ruler.plugin.cdshooks;

import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.JsonArray;

import org.opencds.cqf.ruler.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.ruler.external.annotations.OnR4Condition;
import org.opencds.cqf.ruler.plugin.cdshooks.providers.ProviderConfiguration;
import org.opencds.cqf.ruler.plugin.cql.CqlProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

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
	public ProviderConfiguration providerConfiguration(CdsHooksProperties cdsProperties, CqlProperties cqlProperties) {
		return new ProviderConfiguration(cdsProperties, cqlProperties);
	}

	@Bean(name = "globalCdsServiceCache")
	public AtomicReference<JsonArray> cdsServiceCache() {
		return new AtomicReference<>();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public ServletRegistrationBean<org.opencds.cqf.ruler.plugin.cdshooks.dstu3.CdsHooksServlet> cdsHooksRegistrationBeanDstu3() {
		org.opencds.cqf.ruler.plugin.cdshooks.dstu3.CdsHooksServlet cdsHooksServlet = new org.opencds.cqf.ruler.plugin.cdshooks.dstu3.CdsHooksServlet();
		beanFactory.autowireBean(cdsHooksServlet);

		ServletRegistrationBean<org.opencds.cqf.ruler.plugin.cdshooks.dstu3.CdsHooksServlet> registrationBean = new ServletRegistrationBean<org.opencds.cqf.ruler.plugin.cdshooks.dstu3.CdsHooksServlet>();
		registrationBean.setServlet(cdsHooksServlet);
		registrationBean.addUrlMappings("/cds-services/*");
		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public ServletRegistrationBean<org.opencds.cqf.ruler.plugin.cdshooks.r4.CdsHooksServlet> cdsHooksRegistrationBeanR4() {
		org.opencds.cqf.ruler.plugin.cdshooks.r4.CdsHooksServlet cdsHooksServlet = new org.opencds.cqf.ruler.plugin.cdshooks.r4.CdsHooksServlet();
		beanFactory.autowireBean(cdsHooksServlet);

		ServletRegistrationBean<org.opencds.cqf.ruler.plugin.cdshooks.r4.CdsHooksServlet> registrationBean = new ServletRegistrationBean<org.opencds.cqf.ruler.plugin.cdshooks.r4.CdsHooksServlet>();
		registrationBean.setServlet(cdsHooksServlet);
		registrationBean.addUrlMappings("/cds-services/*");
		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}
}
