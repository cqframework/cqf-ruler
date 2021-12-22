package org.opencds.cqf.ruler.plugin.cdshooks;

import ca.uhn.fhir.jpa.starter.annotations.OnDSTU3Condition;
import com.google.gson.JsonArray;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.fhir.terminology.Dstu3FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.fhir.terminology.R4FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.ruler.plugin.cdshooks.providers.ProviderConfiguration;
import org.opencds.cqf.ruler.plugin.cql.CqlConfig;
import org.opencds.cqf.ruler.plugin.cql.CqlProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import org.springframework.context.annotation.Primary;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.cdshooks", name = "enabled", havingValue = "true")
public class CdsHooksConfig {

	@Bean
	public CdsHooksProperties cdsHooksProperties() {
		return new CdsHooksProperties();
	}

	@Bean
	@Primary
	public CqlConfig cqlConfiguration() { return new CqlConfig(); }

//	@Bean
//	public ProviderConfiguration providerConfiguration() { return ProviderConfiguration.DEFAULT_PROVIDER_CONFIGURATION; }

	@Bean
	public ProviderConfiguration providerConfiguration(CdsHooksProperties cdsProperties, CqlProperties cqlProperties)
	{ return new ProviderConfiguration(cdsProperties, cqlProperties); }

	@Bean(name="globalCdsServiceCache")
	public AtomicReference<JsonArray> cdsServiceCache(){
		return new AtomicReference<>();
	}


	// DSTU 3
	@Bean
	@Primary
	@Conditional(OnDSTU3Condition.class)
	public TerminologyProvider terminologyProviderDstu3() { return new Dstu3FhirTerminologyProvider();
	}

	@Bean
	@Primary
	@Conditional(OnDSTU3Condition.class)
	public ca.uhn.fhir.cql.dstu3.provider.LibraryResolutionProviderImpl libraryResolutionProviderDstu3() { return new ca.uhn.fhir.cql.dstu3.provider.LibraryResolutionProviderImpl(); }

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public org.opencds.cqf.ruler.plugin.cdshooks.dstu3.helpers.LibraryHelper libraryHelperDstu3(
		Map<VersionedIdentifier,Model> modelCache,
		Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> libraryCache,
		CqlTranslatorOptions translatorOptions) { return new org.opencds.cqf.ruler.plugin.cdshooks.dstu3.helpers.LibraryHelper(modelCache, libraryCache, translatorOptions); }

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public ServletRegistrationBean<org.opencds.cqf.ruler.plugin.cdshooks.dstu3.CdsHooksServlet> cdsHooksRegistrationBeanDstu3() {

		AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = new AnnotationConfigWebApplicationContext();
		annotationConfigWebApplicationContext.register(CdsHooksConfig.class);

		org.opencds.cqf.ruler.plugin.cdshooks.dstu3.CdsHooksServlet cdsHooksServlet = new org.opencds.cqf.ruler.plugin.cdshooks.dstu3.CdsHooksServlet();

		ServletRegistrationBean<org.opencds.cqf.ruler.plugin.cdshooks.dstu3.CdsHooksServlet> registrationBean = new ServletRegistrationBean<org.opencds.cqf.ruler.plugin.cdshooks.dstu3.CdsHooksServlet>();
		registrationBean.setServlet(cdsHooksServlet);
		registrationBean.addUrlMappings("/cds-services/*");
		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}


	// R4
	@Bean
	@Primary
	@Conditional(OnR4Condition.class)
	public TerminologyProvider terminologyProviderR4() { return new R4FhirTerminologyProvider();
	}

	@Bean
	@Primary
	@Conditional(OnR4Condition.class)
	public ca.uhn.fhir.cql.r4.provider.LibraryResolutionProviderImpl libraryResolutionProviderR4() { return new ca.uhn.fhir.cql.r4.provider.LibraryResolutionProviderImpl(); }

	@Bean
	@Conditional(OnR4Condition.class)
	public org.opencds.cqf.ruler.plugin.cdshooks.r4.helpers.LibraryHelper libraryHelperR4(
		Map<VersionedIdentifier,Model> modelCache,
		Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> libraryCache,
		CqlTranslatorOptions translatorOptions) { return new org.opencds.cqf.ruler.plugin.cdshooks.r4.helpers.LibraryHelper(modelCache, libraryCache, translatorOptions); }

	@Bean
	@Conditional(OnR4Condition.class)
	public ServletRegistrationBean<org.opencds.cqf.ruler.plugin.cdshooks.r4.CdsHooksServlet> cdsHooksRegistrationBeanR4() {

		AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = new AnnotationConfigWebApplicationContext();
		annotationConfigWebApplicationContext.register(CdsHooksConfig.class);

		org.opencds.cqf.ruler.plugin.cdshooks.r4.CdsHooksServlet cdsHooksServlet = new org.opencds.cqf.ruler.plugin.cdshooks.r4.CdsHooksServlet();

		ServletRegistrationBean<org.opencds.cqf.ruler.plugin.cdshooks.r4.CdsHooksServlet> registrationBean = new ServletRegistrationBean<org.opencds.cqf.ruler.plugin.cdshooks.r4.CdsHooksServlet>();
		registrationBean.setServlet(cdsHooksServlet);
		registrationBean.addUrlMappings("/cds-services/*");
		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}
}

