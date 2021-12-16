package org.opencds.cqf.ruler.plugin.cdshooks;

import ca.uhn.fhir.cql.common.provider.LibraryResolutionProvider;
import ca.uhn.fhir.cql.r4.helper.LibraryHelper;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import com.google.gson.JsonArray;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.codesystems.LibraryType;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.ruler.plugin.cdshooks.providers.ProviderConfiguration;
import org.opencds.cqf.ruler.plugin.cdshooks.r4.CdsHooksServlet;
import org.opencds.cqf.ruler.plugin.cql.CqlConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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

	@Bean(name="globalCdsServiceCache")
	AtomicReference<JsonArray> cdsServiceCache(){
		return new AtomicReference<>();
	}

	//TODO: CQL Plugin - Add Library Operations Provider
//	@Bean
//	public LibraryResolutionProvider<Library> libraryResolutionProvider() { return new LibraryResolutionProvider<Library>() {
//		@Override
//		public Library resolveLibraryById(String s, RequestDetails requestDetails) {
//			return null;
//		}
//
//		@Override
//		public Library resolveLibraryByName(String s, String s1) {
//			return null;
//		}
//
//		@Override
//		public Library resolveLibraryByCanonicalUrl(String s, RequestDetails requestDetails) {
//			return null;
//		}
//
//		@Override
//		public void update(Library library) {
//
//		}
//	};}

	@Bean
	public LibraryHelper libraryHelper(
		Map<VersionedIdentifier,Model> modelCache,
		Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> libraryCache,
		CqlTranslatorOptions translatorOptions) { return new LibraryHelper(modelCache, libraryCache, translatorOptions); }

	@Bean
	@Primary
	public CqlConfig cqlConfiguration() { return new CqlConfig(); }

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

