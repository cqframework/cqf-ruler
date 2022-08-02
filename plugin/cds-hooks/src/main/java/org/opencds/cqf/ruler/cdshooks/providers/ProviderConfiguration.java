package org.opencds.cqf.ruler.cdshooks.providers;

import org.opencds.cqf.ruler.cdshooks.CdsHooksProperties;
import org.opencds.cqf.ruler.cql.CqlProperties;

import ca.uhn.fhir.rest.api.SearchStyleEnum;

public class ProviderConfiguration {

	public static final ProviderConfiguration DEFAULT_PROVIDER_CONFIGURATION = new ProviderConfiguration(true, 64,
			SearchStyleEnum.GET, 8000, false, true);

	private int maxCodesPerQuery;
	private SearchStyleEnum searchStyle;
	private boolean expandValueSets;
	private int maxUriLength;
	private boolean cqlLoggingEnabled;
	private boolean useRemoteData;

	public ProviderConfiguration(boolean expandValueSets, int maxCodesPerQuery, SearchStyleEnum searchStyle,
			int maxUriLength, boolean cqlLoggingEnabled, boolean useRemoteData) {
		this.maxCodesPerQuery = maxCodesPerQuery;
		this.searchStyle = searchStyle;
		this.expandValueSets = expandValueSets;
		this.maxUriLength = maxUriLength;
		this.cqlLoggingEnabled = cqlLoggingEnabled;
		this.useRemoteData = useRemoteData;
	}

	public ProviderConfiguration(CdsHooksProperties cdsProperties, CqlProperties cqlProperties) {
		this.expandValueSets = cdsProperties.getFhirServer().getExpandValueSets();
		this.maxCodesPerQuery = cdsProperties.getFhirServer().getMaxCodesPerQuery();
		this.searchStyle = cdsProperties.getFhirServer().getSearchStyle();
		this.maxUriLength = cdsProperties.getPrefetch().getMaxUriLength();
		this.cqlLoggingEnabled = cqlProperties.getOptions().getCqlEngineOptions().isDebugLoggingEnabled();
		this.useRemoteData = cdsProperties.isUseRemoteData();
	}

	public int getMaxCodesPerQuery() {
		return this.maxCodesPerQuery;
	}

	public SearchStyleEnum getSearchStyle() {
		return this.searchStyle;
	}

	public boolean getExpandValueSets() {
		return this.expandValueSets;
	}

	public int getMaxUriLength() {
		return this.maxUriLength;
	}

	public boolean getCqlLoggingEnabled() {
		return this.cqlLoggingEnabled;
	}

	public boolean getUseRemoteData() {
		return this.useRemoteData;
	}

	public void setUseRemoteData(boolean useRemoteData) {
		this.useRemoteData = useRemoteData;
	}
}
