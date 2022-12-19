package org.opencds.cqf.ruler.cdshooks.providers;

import org.opencds.cqf.ruler.cdshooks.CdsHooksProperties;
import org.opencds.cqf.ruler.cql.CqlProperties;

import ca.uhn.fhir.rest.api.SearchStyleEnum;

public class ProviderConfiguration {

	public static final ProviderConfiguration DEFAULT_PROVIDER_CONFIGURATION = new ProviderConfiguration(true, null,
			SearchStyleEnum.GET, 8000, false, null);

	private Integer maxCodesPerQuery;
	private SearchStyleEnum searchStyle;
	private boolean expandValueSets;
	private Integer queryBatchThreshold;
	private int maxUriLength;
	private boolean cqlLoggingEnabled;

	public ProviderConfiguration(boolean expandValueSets, Integer maxCodesPerQuery, SearchStyleEnum searchStyle,
			int maxUriLength, boolean cqlLoggingEnabled, Integer queryBatchThreshold) {
		this.maxCodesPerQuery = maxCodesPerQuery;
		this.searchStyle = searchStyle;
		this.expandValueSets = expandValueSets;
		this.maxUriLength = maxUriLength;
		this.cqlLoggingEnabled = cqlLoggingEnabled;
		this.queryBatchThreshold = queryBatchThreshold;
	}

	public ProviderConfiguration(CdsHooksProperties cdsProperties, CqlProperties cqlProperties) {
		this.expandValueSets = cdsProperties.getFhirServer().getExpandValueSets();
		this.maxCodesPerQuery = cdsProperties.getFhirServer().getMaxCodesPerQuery();
		this.searchStyle = cdsProperties.getFhirServer().getSearchStyle();
		this.maxUriLength = cdsProperties.getPrefetch().getMaxUriLength();
		this.queryBatchThreshold = cdsProperties.getFhirServer().getQueryBatchThreshold();
		this.cqlLoggingEnabled = cqlProperties.getOptions().getCqlEngineOptions().isDebugLoggingEnabled();
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

	public int getMaxUriLength() {
		return this.maxUriLength;
	}

	public boolean getCqlLoggingEnabled() {
		return this.cqlLoggingEnabled;
	}
}
