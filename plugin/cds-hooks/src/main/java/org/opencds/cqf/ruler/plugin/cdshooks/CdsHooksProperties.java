package org.opencds.cqf.ruler.plugin.cdshooks;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.cdshooks")
public class CdsHooksProperties {

	private Boolean enabled;
	public Boolean getEnabled() { return enabled; }
	public void setEnabled(Boolean enabled) { this.enabled = enabled; }

	private FhirServer fhirServer = new FhirServer();
	public FhirServer getFhirServer() {
		return this.fhirServer;
	}
	public void setFhirServer(FhirServer fhirServer) {
		this.fhirServer = fhirServer;
	}
	public class FhirServer {
		private String maxCodesPerQuery;
		public String getMaxCodesPerQuery() { return maxCodesPerQuery; }
		public void setMaxCodesPerQuery(String maxCodesPerQuery) {
			this.maxCodesPerQuery = maxCodesPerQuery;
		}

		private Boolean expandedValueSets;
		public Boolean getExpandedValueSets() { return expandedValueSets; }
		public void setExpandedValueSets(Boolean expandedValueSets) {
			this.expandedValueSets = expandedValueSets;
		}

		private String searchStyle;
		public String getSearchStyle() { return searchStyle; }
		public void setSearchStyle(String searchStyle) {
			this.searchStyle = searchStyle;
		}
	}

	private Prefetch prefetch = new Prefetch();
	public Prefetch getPrefetch() {
		return this.prefetch;
	}
	public void setPrefetch(Prefetch prefetch) {
		this.prefetch = prefetch;
	}
	public class Prefetch {
		private Integer maxUriLength;
		public Integer getMaxUriLength() { return maxUriLength; }
		public void setMaxUriLength(Integer maxUriLength) {
			this.maxUriLength = maxUriLength;
		}
	}
}

