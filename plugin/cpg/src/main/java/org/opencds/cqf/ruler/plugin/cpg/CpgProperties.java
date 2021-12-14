package org.opencds.cqf.ruler.plugin.cpg;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.cpg")
@EnableConfigurationProperties
public class CpgProperties {

	private Boolean enabled = true;

	public Boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
}
