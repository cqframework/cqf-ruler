package org.opencds.cqf.ruler.casereporting;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.casereporting")
@EnableConfigurationProperties
public class CaseReportingProperties {

	private Boolean enabled = true;

	public Boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
}
