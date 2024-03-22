package org.opencds.cqf.ruler.cr;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@ConfigurationProperties(prefix = "hapi.fhir.rulercr")
@Configuration
@EnableConfigurationProperties
public class CrRulerProperties {
	private boolean enabled = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	private final String rckmsSynonymsUrl = "http://ersd.aimsplatform.org/fhir/ValueSet/rckms-condition-codes";

	public String getRckmsSynonymsUrl() {
		return this.rckmsSynonymsUrl;
	}

	private String vsacUsername;

	public String getVsacUsername() { return vsacUsername; }

	public void setVsacUsername(String vsacUsername) { this.vsacUsername = vsacUsername; }

	private String vsacApiKey;

	public String getVsacApiKey() { return vsacApiKey; }

	public void setVsacApiKey(String vsacApiKey) { this.vsacApiKey = vsacApiKey; }


}
