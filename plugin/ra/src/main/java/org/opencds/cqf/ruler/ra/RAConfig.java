package org.opencds.cqf.ruler.ra;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.external.annotations.OnR4Condition;
import org.opencds.cqf.ruler.ra.r4.AssistedServlet;
import org.opencds.cqf.ruler.ra.r4.ResolveProvider;
import org.opencds.cqf.ruler.ra.r4.RiskAdjustmentProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.ra", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RAConfig {

	@Autowired
	AutowireCapableBeanFactory beanFactory;
	@Bean
	public RAProperties RAProperties() {
		return new RAProperties();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4ReportProvider() {
		return new org.opencds.cqf.ruler.ra.r4.ReportProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4RiskAdjustmentProvider() {
		return new RiskAdjustmentProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4ResolveProvider() {
		return new ResolveProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public ServletRegistrationBean<AssistedServlet> assistedServletServletRegistrationBeanR4() {
		AssistedServlet assistedServlet = new AssistedServlet();
		beanFactory.autowireBean(assistedServlet);
		ServletRegistrationBean<AssistedServlet> registrationBean = new ServletRegistrationBean<>();
		registrationBean.setName("davinci-ra assisted servlet");
		registrationBean.setServlet(assistedServlet);
		registrationBean.addUrlMappings("/assisted");
		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}
}
