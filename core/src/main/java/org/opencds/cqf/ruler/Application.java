package org.opencds.cqf.ruler;


import org.opencds.cqf.ruler.config.BeanFinderConfig;
import org.opencds.cqf.ruler.config.RulerConfig;
import org.opencds.cqf.ruler.config.ServerProperties;
import org.opencds.cqf.ruler.config.TesterUIConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.subscription.match.config.WebsocketDispatcherConfig;

@ServletComponentScan(basePackageClasses = Application.class)
@SpringBootApplication(exclude = { ElasticsearchRestClientAutoConfiguration.class, QuartzAutoConfiguration.class })
@Import({
		RulerConfig.class,
		ServerProperties.class,
		WebsocketDispatcherConfig.class,
		org.opencds.cqf.jpa.starter.mdm.MdmConfig.class,
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
	@Conditional(org.opencds.cqf.jpa.starter.annotations.OnEitherVersion.class)
	public ServletRegistrationBean<Server> hapiServletRegistration() {
		ServletRegistrationBean<Server> servletRegistrationBean = new ServletRegistrationBean<>();
		Server server = new Server();
		beanFactory.autowireBean(server);
		servletRegistrationBean.setName("fhir servlet");
		servletRegistrationBean.setServlet(server);
		servletRegistrationBean.addUrlMappings("/fhir/*");
		servletRegistrationBean.setLoadOnStartup(1);

		return servletRegistrationBean;
	}
}
