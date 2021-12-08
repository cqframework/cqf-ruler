package org.opencds.cqf.ruler.plugin.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

public class SecurityProperties {

	private Boolean enabled;
	public Boolean getEnabled() { return enabled; }
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	private Boolean securityCors;
	public Boolean getSecurityCors() { return securityCors; }
	public void setSecurityCors(Boolean securityCors) {
		this.securityCors = securityCors;
	}

	private String securityUrl;
	public String getSecurityUrl() { return securityUrl; }
	public void setSecurityUrl(String securityUrl) {
		this.securityUrl = securityUrl;
	}

	private String securityExtAuthUrl;
	public String getSecurityExtAuthUrl() { return securityExtAuthUrl; }
	public void setSecurityExtAuthUrl(String securityExtAuthUrl) {
		this.securityExtAuthUrl = securityExtAuthUrl;
	}

	private String securityExtAuthValueUri;
	public String getSecurityExtAuthValueUri() { return securityExtAuthValueUri; }
	public void setSecurityExtAuthValueUri(String securityExtAuthValueUri) { this.securityExtAuthValueUri = securityExtAuthValueUri; }

	private String securityExtTokenUrl;
	public String getSecurityExtTokenUrl() { return securityExtTokenUrl; }
	public void setSecurityExtTokenUrl(String securityExtTokenUrl) {
		this.securityExtTokenUrl = securityExtTokenUrl;
	}

	private String securityExtTokenValueUri;
	public String getSecurityExtTokenValueUri() { return securityExtTokenValueUri; }
	public void setSecurityExtTokenValueUri(String securityExtTokenValueUri) { this.securityExtTokenValueUri = securityExtTokenValueUri; }

	private String serviceSystem;
	public String getServiceSystem() { return serviceSystem; }
	public void setServiceSystem(String serviceSystem) {
		this.serviceSystem = serviceSystem;
	}

	private String serviceCode;
	public String getServiceCode() { return serviceCode; }
	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
	}

	private String serviceDisplay;
	public String getServiceDisplay() { return serviceDisplay; }
	public void setServiceDisplay(String serviceDisplay) {
		this.serviceDisplay = serviceDisplay;
	}

	private String serviceText;
	public String getServiceText() { return serviceText; }
	public void setServiceText(String serviceText) {
		this.serviceText = serviceText;
	}
    
}

