package org.opencds.cqf.ruler.plugin.cpg;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.cpg")
@EnableConfigurationProperties
public class CpgProperties {

	private Boolean enabled = true;
	
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
    
}
