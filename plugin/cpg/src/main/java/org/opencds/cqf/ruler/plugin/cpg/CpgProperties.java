package org.opencds.cqf.ruler.plugin.cpg;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.cpg")
@EnableConfigurationProperties
public class CpgProperties {

	private Boolean enabled = true;

	private Boolean log_enabled = false;

	private Boolean use_embedded_cql_translator_content = true;

	private Boolean cql_debug_enabled = false;

	private CqlTranslatorOptions cqlTranslatorOptions = CqlTranslatorOptions.defaultOptions();

	public Boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Boolean getLogEnabled() {
		return this.log_enabled;
	}

	public void setLogEnabled(Boolean log_enabled) {
		this.log_enabled = log_enabled;
	}

	public Boolean getCql_debug_enabled() {
		return cql_debug_enabled;
	}

	public void setCql_debug_enabled(Boolean cql_debug_enabled) {
		this.cql_debug_enabled = cql_debug_enabled;
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
