package org.opencds.cqf.ruler.plugin.cdshooks.providers;

import ca.uhn.fhir.rest.api.SearchStyleEnum;

public class ProviderConfiguration {

    public static final ProviderConfiguration DEFAULT_PROVIDER_CONFIGURATION =
        new ProviderConfiguration(true, 64, SearchStyleEnum.GET, 8000, false);

    private int maxCodesPerQuery;
    private SearchStyleEnum searchStyle;
    private boolean expandValueSets;
    private int maxUriLength;
    private boolean cqlLoggingEnabled;

    public ProviderConfiguration(boolean expandValueSets, int maxCodesPerQuery, SearchStyleEnum searchStyle, int maxUriLength, boolean cqlLoggingEnabled) {
        this.maxCodesPerQuery = maxCodesPerQuery;
        this.searchStyle = searchStyle;
        this.expandValueSets = expandValueSets;
        this.maxUriLength = maxUriLength;
        this.cqlLoggingEnabled = cqlLoggingEnabled;
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

    public boolean getCqlLoggingEnabled() { return this.cqlLoggingEnabled; }
}
