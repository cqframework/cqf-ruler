package org.opencds.cqf.ruler.cdshooks;

import ca.uhn.fhir.cr.config.CrProperties;
import ca.uhn.fhir.cr.r4.IPlanDefinitionProcessorFactory;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.library.EvaluationSettings;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.PlanDefinitionProcessor;
import org.opencds.cqf.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.external.annotations.OnR4Condition;
import org.opencds.cqf.ruler.cdshooks.providers.ProviderConfiguration;
import org.opencds.cqf.ruler.cpg.CpgConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.cache.IResourceChangeListenerRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;

@Configuration
@Import({CpgConfig.class,
})
@ConditionalOnProperty(prefix = "hapi.fhir.cdshooks", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CdsHooksConfig {

	@Autowired
	AutowireCapableBeanFactory beanFactory;

	/*@Bean
	@Conditional(OnR4Condition.class)
	public ca.uhn.fhir.cr.r4.IActivityDefinitionProcessorFactory r4ActivityDefinitionProcessorFactory(EvaluationSettings theEvaluationSettings) {
		return r -> new org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor(r, theEvaluationSettings);
	}
	@Bean
	@Conditional(OnR4Condition.class)
	ca.uhn.fhir.cr.r4.IPlanDefinitionProcessorFactory r4PlanDefinitionProcessorFactory(EvaluationSettings theEvaluationSettings) {
		return r -> new org.opencds.cqf.cql.evaluator.plandefinition.r4.PlanDefinitionProcessor(r, theEvaluationSettings);
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public ca.uhn.fhir.cr.dstu3.IActivityDefinitionProcessorFactory dstu3ActivityDefinitionProcessorFactory(EvaluationSettings theEvaluationSettings) {
		return r -> new org.opencds.cqf.cql.evaluator.activitydefinition.dstu3.ActivityDefinitionProcessor(r, theEvaluationSettings);
	}
	@Bean
	@Conditional(OnDSTU3Condition.class)
	ca.uhn.fhir.cr.dstu3.IPlanDefinitionProcessorFactory dstu3PlanDefinitionProcessorFactory(EvaluationSettings theEvaluationSettings) {
		return r -> new org.opencds.cqf.cql.evaluator.plandefinition.dstu3.PlanDefinitionProcessor(r, theEvaluationSettings);
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public ca.uhn.fhir.cr.r4.activitydefinition.ActivityDefinitionOperationsProvider r4PlanDefinitionApplyProvider() {
		return new ca.uhn.fhir.cr.r4.activitydefinition.ActivityDefinitionOperationsProvider();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public ca.uhn.fhir.cr.dstu3.activitydefinition.ActivityDefinitionOperationsProvider dstu3PlanDefinitionApplyProvider() {
		return new ca.uhn.fhir.cr.dstu3.activitydefinition.ActivityDefinitionOperationsProvider();
	}
*/
	@Bean
	public CdsHooksProperties cdsHooksProperties() {
		return new CdsHooksProperties();
	}

	@Bean
	public ProviderConfiguration providerConfiguration(CdsHooksProperties cdsProperties, CrProperties.CqlProperties cqlProperties) {
		return new ProviderConfiguration(cdsProperties, cqlProperties);
	}

	@Bean
	public CdsServicesCache cdsServiceInterceptor(IResourceChangeListenerRegistry resourceChangeListenerRegistry,
			DaoRegistry daoRegistry) {
		CdsServicesCache listener = new CdsServicesCache(daoRegistry);
		resourceChangeListenerRegistry.registerResourceResourceChangeListener("PlanDefinition",
				SearchParameterMap.newSynchronous(), listener, 1000);
		return listener;
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	@DependsOn({ "dstu3CqlExecutionProvider", "dstu3LibraryEvaluationProvider" })
	public ServletRegistrationBean<org.opencds.cqf.ruler.cdshooks.dstu3.CdsHooksServlet> cdsHooksRegistrationBeanDstu3() {
		org.opencds.cqf.ruler.cdshooks.dstu3.CdsHooksServlet cdsHooksServlet = new org.opencds.cqf.ruler.cdshooks.dstu3.CdsHooksServlet();
		beanFactory.autowireBean(cdsHooksServlet);

		ServletRegistrationBean<org.opencds.cqf.ruler.cdshooks.dstu3.CdsHooksServlet> registrationBean = new ServletRegistrationBean<>();
		registrationBean.setName("cds-hooks servlet");
		registrationBean.setServlet(cdsHooksServlet);
		registrationBean.addUrlMappings("/cds-services/*");
		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	@Conditional(OnR4Condition.class)
	@DependsOn({ "r4CqlExecutionProvider", "r4LibraryEvaluationProvider" })
	public ServletRegistrationBean<org.opencds.cqf.ruler.cdshooks.r4.CdsHooksServlet> cdsHooksRegistrationBeanR4() {
		org.opencds.cqf.ruler.cdshooks.r4.CdsHooksServlet cdsHooksServlet = new org.opencds.cqf.ruler.cdshooks.r4.CdsHooksServlet();
		beanFactory.autowireBean(cdsHooksServlet);

		ServletRegistrationBean<org.opencds.cqf.ruler.cdshooks.r4.CdsHooksServlet> registrationBean = new ServletRegistrationBean<>();
		registrationBean.setName("cds-hooks servlet");
		registrationBean.setServlet(cdsHooksServlet);
		registrationBean.addUrlMappings("/cds-services/*");
		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}
}
