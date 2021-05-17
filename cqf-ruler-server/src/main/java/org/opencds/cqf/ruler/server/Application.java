package org.opencds.cqf.ruler.server;

import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig;
import ca.uhn.fhir.jpa.subscription.match.config.WebsocketDispatcherConfig;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;

import org.opencds.cqf.ruler.server.annotations.OnEitherVersion;
import org.opencds.cqf.ruler.server.mdm.MdmConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

@ServletComponentScan(basePackageClasses = {
  JpaRestfulServer.class})
@SpringBootApplication(exclude = {ElasticsearchRestClientAutoConfiguration.class})
@Import({SubscriptionSubmitterConfig.class, SubscriptionProcessorConfig.class, SubscriptionChannelConfig.class, WebsocketDispatcherConfig.class, MdmConfig.class})
public class Application extends SpringBootServletInitializer {

  public static void main(String[] args) {

    System.setProperty("spring.batch.job.enabled", "false");
    SpringApplication.run(Application.class, args);

    //Server is now accessible at eg. http://localhost:8080/fhir/metadata
    //UI is now accessible at http://localhost:8080/
  }

  @Override
  protected SpringApplicationBuilder configure(
    SpringApplicationBuilder builder) {
    return builder.sources(Application.class);
  }

  @Autowired
  AutowireCapableBeanFactory beanFactory;

  @Bean
  @Conditional(OnEitherVersion.class)
  @SuppressWarnings("rawtypes")
  public ServletRegistrationBean hapiServletRegistration() {
    ServletRegistrationBean<JpaRestfulServer> servletRegistrationBean = new ServletRegistrationBean<JpaRestfulServer>();
    JpaRestfulServer jpaRestfulServer = new JpaRestfulServer();
    beanFactory.autowireBean(jpaRestfulServer);
    servletRegistrationBean.setServlet(jpaRestfulServer);
    servletRegistrationBean.addUrlMappings("/fhir/*");
    servletRegistrationBean.setLoadOnStartup(1);

    return servletRegistrationBean;
  }

  @Bean
  @SuppressWarnings("rawtypes")
  public ServletRegistrationBean overlayRegistrationBean() {

    AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = new AnnotationConfigWebApplicationContext();
    annotationConfigWebApplicationContext.register(FhirTesterConfig.class);

    DispatcherServlet dispatcherServlet = new DispatcherServlet(
      annotationConfigWebApplicationContext);
    dispatcherServlet.setContextClass(AnnotationConfigWebApplicationContext.class);
    dispatcherServlet.setContextConfigLocation(FhirTesterConfig.class.getName());

    ServletRegistrationBean<DispatcherServlet> registrationBean = new ServletRegistrationBean<DispatcherServlet>();
    registrationBean.setServlet(dispatcherServlet);
    registrationBean.addUrlMappings("/*");
    registrationBean.setLoadOnStartup(2);
    return registrationBean;

  }

  // @Bean
  // @SuppressWarnings("rawtypes")
  // @Conditional(OnR4Condition.class)
  // public ServletRegistrationBean cdsHooksR4Bean() {
  //   ServletRegistrationBean<org.opencds.cqf.ruler.r4.servlet.CdsHooksServlet> registrationBean = new ServletRegistrationBean<org.opencds.cqf.ruler.r4.servlet.CdsHooksServlet>();
  //   org.opencds.cqf.ruler.r4.servlet.CdsHooksServlet cdsHooks = new org.opencds.cqf.ruler.r4.servlet.CdsHooksServlet();
  //   beanFactory.autowireBean(cdsHooks);
  //   registrationBean.setServlet(cdsHooks);
  //   registrationBean.addUrlMappings("/cds-services/*");
  //   registrationBean.setLoadOnStartup(3);
  //   return registrationBean;
  // }

  // @Bean
  // @SuppressWarnings("rawtypes")
  // @Conditional(OnDSTU3Condition.class)
  // public ServletRegistrationBean cdsHooksDstu3Bean() {
  //   ServletRegistrationBean<org.opencds.cqf.ruler.dstu3.servlet.CdsHooksServlet> registrationBean = new ServletRegistrationBean<org.opencds.cqf.ruler.dstu3.servlet.CdsHooksServlet>();
  //   org.opencds.cqf.ruler.dstu3.servlet.CdsHooksServlet cdsHooks = new org.opencds.cqf.ruler.dstu3.servlet.CdsHooksServlet();
  //   beanFactory.autowireBean(cdsHooks);
  //   registrationBean.setServlet(cdsHooks);
  //   registrationBean.addUrlMappings("/cds-services/*");
  //   registrationBean.setLoadOnStartup(3);
  //   return registrationBean;
  // }
}
