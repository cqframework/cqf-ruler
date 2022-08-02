package org.opencds.cqf.ruler.cdshooks;

import org.springframework.boot.context.properties.ConfigurationProperties;

import ca.uhn.fhir.rest.api.SearchStyleEnum;

@ConfigurationProperties(prefix = "hapi.fhir.cdshooks")
public class CdsHooksProperties {

	private boolean enabled;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	private FhirServer fhirServer = new FhirServer();

	public FhirServer getFhirServer() {
		return this.fhirServer;
	}

	public void setFhirServer(FhirServer fhirServer) {
		this.fhirServer = fhirServer;
	}

	public class FhirServer {
		private Integer maxCodesPerQuery;

		public Integer getMaxCodesPerQuery() {
			return maxCodesPerQuery;
		}

		public void setMaxCodesPerQuery(Integer maxCodesPerQuery) {
			this.maxCodesPerQuery = maxCodesPerQuery;
		}

		private boolean expandValueSets;

		public boolean getExpandValueSets() {
			return expandValueSets;
		}

		public void setExpandValueSets(boolean expandValueSets) {
			this.expandValueSets = expandValueSets;
		}

		private SearchStyleEnum searchStyle;

		public SearchStyleEnum getSearchStyle() {
			return searchStyle;
		}

		public void setSearchStyle(SearchStyleEnum searchStyle) {
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

		public Integer getMaxUriLength() {
			return maxUriLength;
		}

		public void setMaxUriLength(Integer maxUriLength) {
			this.maxUriLength = maxUriLength;
		}
	}

	private boolean useRemoteData;

	public boolean isUseRemoteData() {
		return useRemoteData;
	}

	public void setUseRemoteData(boolean useRemoteData) {
		this.useRemoteData = useRemoteData;
	}
}
