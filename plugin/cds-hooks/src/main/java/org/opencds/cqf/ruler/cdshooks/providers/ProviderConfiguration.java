package org.opencds.cqf.ruler.cdshooks.providers;

import org.opencds.cqf.ruler.cdshooks.CdsHooksProperties;


import ca.uhn.fhir.rest.api.SearchStyleEnum;

public class ProviderConfiguration {

	public static final ProviderConfiguration DEFAULT_PROVIDER_CONFIGURATION = new ProviderConfiguration(true, 64,
			SearchStyleEnum.GET, 8000, 5, "client_id");

	private final Integer maxCodesPerQuery;
	private final SearchStyleEnum searchStyle;
	private final boolean expandValueSets;
	private final Integer queryBatchThreshold;
	private final Integer maxUriLength;
	private final String clientIdHeaderName;

	public ProviderConfiguration(boolean expandValueSets, Integer maxCodesPerQuery, SearchStyleEnum searchStyle,
			Integer maxUriLength, Integer queryBatchThreshold, String clientIdHeaderName) {
		this.maxCodesPerQuery = maxCodesPerQuery;
		this.searchStyle = searchStyle;
		this.expandValueSets = expandValueSets;
		this.maxUriLength = maxUriLength;
		this.queryBatchThreshold = queryBatchThreshold;
		this.clientIdHeaderName = clientIdHeaderName;
	}

	public ProviderConfiguration(CdsHooksProperties cdsProperties) {
		this.expandValueSets = cdsProperties.getFhirServer().getExpandValueSets();
		this.maxCodesPerQuery = cdsProperties.getFhirServer().getMaxCodesPerQuery();
		this.searchStyle = cdsProperties.getFhirServer().getSearchStyle();
		this.maxUriLength = cdsProperties.getPrefetch().getMaxUriLength();
		this.queryBatchThreshold = cdsProperties.getFhirServer().getQueryBatchThreshold();
		this.clientIdHeaderName = cdsProperties.getClientIdHeaderName();
	}

	public Integer getMaxCodesPerQuery() {
		return this.maxCodesPerQuery;
	}

	public SearchStyleEnum getSearchStyle() {
		return this.searchStyle;
	}

	public boolean getExpandValueSets() {
		return this.expandValueSets;
	}

	public Integer getQueryBatchThreshold() { return this.queryBatchThreshold; }

	public Integer getMaxUriLength() {
		return this.maxUriLength;
	}

	public String getClientIdHeaderName() {
		return this.clientIdHeaderName;
	}
}
