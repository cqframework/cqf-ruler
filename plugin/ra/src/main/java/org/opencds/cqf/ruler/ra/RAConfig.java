package org.opencds.cqf.ruler.ra;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.cr.config.CrR4Config;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import org.opencds.cqf.external.annotations.OnR4Condition;
import org.opencds.cqf.external.cr.PostInitProviderRegisterer;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.ra.r4.ApproveProvider;
import org.opencds.cqf.ruler.ra.r4.AssistedServlet;
import org.opencds.cqf.ruler.ra.r4.RACodingGapsProvider;
import org.opencds.cqf.ruler.ra.r4.RemediateProvider;
import org.opencds.cqf.ruler.ra.r4.ResolveProvider;
import org.opencds.cqf.ruler.ra.r4.RiskAdjustmentProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.ra", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RAConfig {

	@Bean
	public RAProperties RAProperties() {
		return new RAProperties();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4ReportProvider() {
		return new RACodingGapsProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4RiskAdjustmentProvider() {
		return new RiskAdjustmentProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4RemediateProvider() {
		return new RemediateProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4ApproveProvider() {
		return new ApproveProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4ResolveProvider() {
		return new ResolveProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public ServletRegistrationBean<AssistedServlet> assistedServletServletRegistrationBeanR4(
			AutowireCapableBeanFactory beanFactory) {
		AssistedServlet assistedServlet = new AssistedServlet();
		beanFactory.autowireBean(assistedServlet);
		ServletRegistrationBean<AssistedServlet> registrationBean = new ServletRegistrationBean<>();
		registrationBean.setName("davinci-ra assisted servlet");
		registrationBean.setServlet(assistedServlet);
		registrationBean.addUrlMappings("/assisted");
		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	RAProviderFactory raOperationFactory() {
		return new RAProviderFactory();
	}

	@Bean
	RAProviderLoader raProviderLoader(FhirContext theFhirContext, ResourceProviderFactory theResourceProviderFactory,
												 RAProviderFactory theRAProviderFactory, PostInitProviderRegisterer thePostInitProviderRegisterer) {
		return new RAProviderLoader(theFhirContext, theResourceProviderFactory, theRAProviderFactory, thePostInitProviderRegisterer);
	}
}
