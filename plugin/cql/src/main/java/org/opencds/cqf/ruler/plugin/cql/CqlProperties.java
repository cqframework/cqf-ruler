package org.opencds.cqf.ruler.plugin.cql;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.cql")
public class CqlProperties {

    private Boolean enabled = true;

    private Boolean use_embedded_cql_translator_content = true;

    private CqlTranslatorOptions cqlTranslatorOptions = CqlTranslatorOptions.defaultOptions();

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getUse_embedded_cql_translator_content() {
        return this.use_embedded_cql_translator_content;
    }

    public void setUse_embedded_cql_translator_content(Boolean use_embedded_cql_translator_content) {
        this.use_embedded_cql_translator_content = use_embedded_cql_translator_content;
    }

    public CqlTranslatorOptions getCqlTranslatorOptions() {
        return this.cqlTranslatorOptions;
    }

    public void setCqlTranslatorOptions(CqlTranslatorOptions cqlTranslatorOptions) {
        this.cqlTranslatorOptions = cqlTranslatorOptions;
    }
}
