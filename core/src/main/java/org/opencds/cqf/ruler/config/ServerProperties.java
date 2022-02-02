package org.opencds.cqf.ruler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "hapi.fhir")
@Configuration
@EnableConfigurationProperties
public class ServerProperties {
	private String implementation_description = null;

	public String getImplementation_description() {
		return implementation_description;
	 }
  
	 public void setImplementation_description(String implementation_description) {
		this.implementation_description = implementation_description;
	 }

	
}
