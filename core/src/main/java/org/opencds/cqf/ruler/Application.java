package org.opencds.cqf.ruler;

import org.opencds.cqf.external.annotations.OnEitherVersion;
import org.opencds.cqf.external.mdm.MdmConfig;
import org.opencds.cqf.ruler.config.BeanFinderConfig;
import org.opencds.cqf.ruler.config.RulerConfig;
import org.opencds.cqf.ruler.config.ServerProperties;
import org.opencds.cqf.ruler.config.TesterUIConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.subscription.match.config.WebsocketDispatcherConfig;
import ca.uhn.fhir.rest.server.RestfulServer;

@ServletComponentScan(basePackageClasses = RestfulServer.class)
@SpringBootApplication(exclude = { ElasticsearchRestClientAutoConfiguration.class, ThymeleafAutoConfiguration.class })
@Import({
		RulerConfig.class,
		ServerProperties.class,
		WebsocketDispatcherConfig.class,
		MdmConfig.class,
		TesterUIConfig.class,
		BeanFinderConfig.class, })
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
		// Server is now accessible at eg. http://localhost:8080/fhir/metadata
		// UI (if enabled) is now accessible at http://localhost:8080/
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(Application.class);
	}

	@Autowired
	AutowireCapableBeanFactory beanFactory;

	@Bean
	@Conditional(OnEitherVersion.class)
	public ServletRegistrationBean hapiServletRegistration(RestfulServer restfulServer) {
		var servletRegistrationBean = new ServletRegistrationBean();
		beanFactory.autowireBean(restfulServer);
		servletRegistrationBean.setServlet(restfulServer);
		servletRegistrationBean.addUrlMappings("/fhir/*");
		servletRegistrationBean.setLoadOnStartup(1);

		return servletRegistrationBean;
	}
}
