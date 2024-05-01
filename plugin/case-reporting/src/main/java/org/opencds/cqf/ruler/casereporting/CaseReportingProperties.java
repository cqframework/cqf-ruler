package org.opencds.cqf.ruler.casereporting;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.casereporting")
@EnableConfigurationProperties
public class CaseReportingProperties {

	private boolean enabled = true;
	private final String rckmsSynonymsUrl = "http://ersd.aimsplatform.org/fhir/ValueSet/rckms-condition-codes";

	public String getRckmsSynonymsUrl() {
		return this.rckmsSynonymsUrl;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
