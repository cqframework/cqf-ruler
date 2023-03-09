package org.opencds.cqf.ruler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "hapi.fhir")
@Configuration
@EnableConfigurationProperties
public class ServerProperties {
	private String implementation_description = null;
	private int max_includes_per_page = 1000;

	public String getImplementation_description() {
		return implementation_description;
	}

	public void setImplementation_description(String implementation_description) {
		this.implementation_description = implementation_description;
	}

	public int getMaxIncludesPerPage() {
		return this.max_includes_per_page;
	}

	public void setMaxIncludesPerPage(int max_includes_per_page) {
		this.max_includes_per_page = max_includes_per_page;
	}

}
