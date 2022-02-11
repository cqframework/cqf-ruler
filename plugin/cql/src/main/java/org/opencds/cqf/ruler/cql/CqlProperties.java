package org.opencds.cqf.ruler.cql;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.cql")
public class CqlProperties {

	private boolean enabled = true;

	private boolean use_embedded_cql_translator_content = true;

	private boolean cql_logging_enabled = true;

	private CqlTranslatorOptions cqlTranslatorOptions = CqlTranslatorOptions.defaultOptions();

	public boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean getUse_embedded_cql_translator_content() {
		return this.use_embedded_cql_translator_content;
	}

	public void setUse_embedded_cql_translator_content(boolean use_embedded_cql_translator_content) {
		this.use_embedded_cql_translator_content = use_embedded_cql_translator_content;
	}

	public CqlTranslatorOptions getCqlTranslatorOptions() {
		return this.cqlTranslatorOptions;
	}

	public void setCqlTranslatorOptions(CqlTranslatorOptions cqlTranslatorOptions) {
		this.cqlTranslatorOptions = cqlTranslatorOptions;
	}

	public boolean getCql_logging_enabled() {
		return cql_logging_enabled;
	}

	public void setCql_logging_enabled(boolean cql_logging_enabled) {
		this.cql_logging_enabled = cql_logging_enabled;
	}
}
