package org.opencds.cqf.ruler.plugin.cpg;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.cpg")
@EnableConfigurationProperties
public class CpgProperties {

	private Boolean enabled = true;

    private Boolean cql_debug_enabled = false;
	
    private CqlTranslatorOptions cqlTranslatorOptions = CqlTranslatorOptions.defaultOptions();

	public Boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

    public CqlTranslatorOptions getCqlTranslatorOptions() {
        return this.cqlTranslatorOptions;
    }

    public void setCqlTranslatorOptions(CqlTranslatorOptions cqlTranslatorOptions) {
        this.cqlTranslatorOptions = cqlTranslatorOptions;
    }

    public Boolean getCql_debug_enabled() {
      return cql_debug_enabled;
    }
  
    public void setCql_debug_enabled(Boolean cql_debug_enabled) {
      this.cql_debug_enabled = cql_debug_enabled;
    }
    
}
