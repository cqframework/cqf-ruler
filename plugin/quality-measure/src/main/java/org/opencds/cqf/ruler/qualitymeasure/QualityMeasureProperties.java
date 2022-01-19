package org.opencds.cqf.ruler.qualitymeasure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.qualitymeasure")
@EnableConfigurationProperties
public class QualityMeasureProperties {

	private Boolean enabled = true;

	public Boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
}
