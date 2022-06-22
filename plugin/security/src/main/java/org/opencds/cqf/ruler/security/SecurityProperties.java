package org.opencds.cqf.ruler.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.security")
public class SecurityProperties {

	private boolean enabled = false;

	private BasicAuth basic_auth;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public BasicAuth getBasicAuth() {
		return this.basic_auth;
	}

	public void setBasicAuth(BasicAuth basic_auth) {
		this.basic_auth = basic_auth;
	}

	private OAuth oAuth = new OAuth();

	public OAuth getOAuth() {
		return this.oAuth;
	}

	public void setOAuth(OAuth oAuth) {
		this.oAuth = oAuth;
	}

	public static class BasicAuth {

		private boolean enabled = false;
		private String username;
		private String password;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getUsername() {
			return this.username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}

	public class OAuth {
		private boolean securityCors = true;
		private boolean enabled = false;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean getSecurityCors() {
			return securityCors;
		}

		public void setSecurityCors(boolean securityCors) {
			this.securityCors = securityCors;
		}

		private String securityUrl;

		public String getSecurityUrl() {
			return securityUrl;
		}

		public void setSecurityUrl(String securityUrl) {
			this.securityUrl = securityUrl;
		}

		private String securityExtAuthUrl;

		public String getSecurityExtAuthUrl() {
			return securityExtAuthUrl;
		}

		public void setSecurityExtAuthUrl(String securityExtAuthUrl) {
			this.securityExtAuthUrl = securityExtAuthUrl;
		}

		private String securityExtAuthValueUri;

		public String getSecurityExtAuthValueUri() {
			return securityExtAuthValueUri;
		}

		public void setSecurityExtAuthValueUri(String securityExtAuthValueUri) {
			this.securityExtAuthValueUri = securityExtAuthValueUri;
		}

		private String securityExtTokenUrl;

		public String getSecurityExtTokenUrl() {
			return securityExtTokenUrl;
		}

		public void setSecurityExtTokenUrl(String securityExtTokenUrl) {
			this.securityExtTokenUrl = securityExtTokenUrl;
		}

		private String securityExtTokenValueUri;

		public String getSecurityExtTokenValueUri() {
			return securityExtTokenValueUri;
		}

		public void setSecurityExtTokenValueUri(String securityExtTokenValueUri) {
			this.securityExtTokenValueUri = securityExtTokenValueUri;
		}

		private String serviceSystem;

		public String getServiceSystem() {
			return serviceSystem;
		}

		public void setServiceSystem(String serviceSystem) {
			this.serviceSystem = serviceSystem;
		}

		private String serviceCode;

		public String getServiceCode() {
			return serviceCode;
		}

		public void setServiceCode(String serviceCode) {
			this.serviceCode = serviceCode;
		}

		private String serviceDisplay;

		public String getServiceDisplay() {
			return serviceDisplay;
		}

		public void setServiceDisplay(String serviceDisplay) {
			this.serviceDisplay = serviceDisplay;
		}

		private String serviceText;

		public String getServiceText() {
			return serviceText;
		}

		public void setServiceText(String serviceText) {
			this.serviceText = serviceText;
		}
	}
}
