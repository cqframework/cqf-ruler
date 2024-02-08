package org.opencds.cqf.ruler.cdshooks;

import ca.uhn.fhir.cr.common.ILibraryLoaderFactory;
import ca.uhn.fhir.cr.config.CrProperties;
import ca.uhn.fhir.interceptor.api.IInterceptorService;
import ca.uhn.fhir.jpa.dao.ITransactionProcessorVersionAdapter;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.quick.FhirLibrarySourceProvider;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.external.annotations.OnR4Condition;
import org.opencds.cqf.ruler.cdshooks.providers.ProviderConfiguration;
import org.opencds.cqf.ruler.cdshooks.providers.TranslatingLibraryLoader;
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
import org.springframework.context.annotation.Scope;

import java.util.Map;

@Configuration
@Import({CpgConfig.class,
})
@ConditionalOnProperty(prefix = "hapi.fhir.cdshooks", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CdsHooksConfig {

	@Autowired
	AutowireCapableBeanFactory beanFactory;

	@Bean
	public CdsHooksProperties cdsHooksProperties() {
		return new CdsHooksProperties();
	}

	@Bean
	public CrProperties crProperties() {
		CrProperties crProperties = new CrProperties();
		crProperties.getCqlProperties().getCqlTranslatorOptions().setEnableCqlOnly(false);
		return crProperties;
	}

	@Bean
	public ProviderConfiguration providerConfiguration(CdsHooksProperties cdsProperties, CrProperties.CqlProperties cqlProperties) {
		cqlProperties.getCqlTranslatorOptions().setEnableCqlOnly(false);
		return new ProviderConfiguration(cdsProperties, cqlProperties);
	}

	@Bean
	@Scope("prototype")
	ILibraryLoaderFactory libraryLoaderFactory(
		Map<VersionedIdentifier, Library> theGlobalLibraryCache,
		ModelManager theModelManager, CqlTranslatorOptions theCqlTranslatorOptions, CrProperties.CqlProperties theCqlProperties) {
		return lcp -> {

			if (theCqlProperties.getCqlOptions().useEmbeddedLibraries()) {
				lcp.add(new FhirLibrarySourceProvider());
			}

			return new TranslatingLibraryLoader(theModelManager, lcp, theCqlTranslatorOptions, theGlobalLibraryCache);
		};
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
	public CDSHooksTransactionInterceptor cdsHooksTransactionInterceptor(DaoRegistry daoRegistry, IInterceptorService interceptorRegistry) {
		CDSHooksTransactionInterceptor interceptor = new CDSHooksTransactionInterceptor(daoRegistry);
		interceptorRegistry.registerInterceptor(interceptor);
		return interceptor;
	}

	@Bean
	public LibraryLoaderCache libraryInterceptor(IResourceChangeListenerRegistry resourceChangeListenerRegistry,
																 DaoRegistry daoRegistry) {
		LibraryLoaderCache listener = new LibraryLoaderCache(daoRegistry);
		resourceChangeListenerRegistry.registerResourceResourceChangeListener("Library",
			SearchParameterMap.newSynchronous(), listener, 1000);
		return listener;
	}

	@Bean
	public ValueSetCache valueSetInterceptor(IResourceChangeListenerRegistry resourceChangeListenerRegistry,
																DaoRegistry daoRegistry) {
		ValueSetCache listener = new ValueSetCache(daoRegistry);
		resourceChangeListenerRegistry.registerResourceResourceChangeListener("ValueSet",
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
