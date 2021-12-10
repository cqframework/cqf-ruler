package org.opencds.cqf.ruler.plugin.cdshooks;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.cdshooks")
public class CdsHooksProperties {

	private Boolean enabled;
	public Boolean getEnabled() { return enabled; }
	public void setEnabled(Boolean enabled) { this.enabled = enabled; }

	private FhirServer fhirserver = new FhirServer();
	public FhirServer getOAuth() {
		return this.fhirserver;
	}
	public void setOAuth(FhirServer fhirserver) {
		this.fhirserver = fhirserver;
	}
	public class FhirServer {
		private String maxCodesPerQuery;
		public String getMaxCodesPerQuery() { return maxCodesPerQuery; }
		public void setMaxCodesPerQuery(String maxCodesPerQuery) { this.maxCodesPerQuery = maxCodesPerQuery; }

		private String expandValueSets;
		public String getExpandValueSets() { return expandValueSets; }
		public void setExpandValueSets(String expandValueSets) { this.expandValueSets = expandValueSets; }

		private String searchStyle;
		public String getSearchStyle() { return searchStyle; }
		public void setSearchStyle(String searchStyle) { this.searchStyle = searchStyle; }

		private String maxUriLength;
		public String getMaxUriLength() { return maxUriLength; }
		public void setMaxUriLength(String maxUriLength) { this.maxUriLength = maxUriLength; }
	}

}

