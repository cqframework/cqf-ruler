package org.opencds.cqf.ruler.cql;

import org.opencds.cqf.cql.evaluator.CqlOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.cql")
public class CqlProperties {

	private boolean enabled = true;

	private CqlOptions cqlOptions = CqlOptions.defaultOptions();

	public boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public CqlOptions getOptions() {
		return this.cqlOptions;
	}

	public void setOptions(CqlOptions cqlOptions) {
		this.cqlOptions = cqlOptions;
	}
}
