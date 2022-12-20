package org.opencds.cqf.ruler.ra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "hapi.fhir.ra")
@Configuration
@EnableConfigurationProperties
public class RAProperties {

	private boolean enabled = true;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	private Report report;

	public Report getReport() {
		return report;
	}

	public void setReport(Report report) {
		this.report = report;
	}

	private CompositionConfiguration compositionConfiguration;

	public CompositionConfiguration getComposition() {
		return compositionConfiguration;
	}

	public void setComposition(CompositionConfiguration compositionConfiguration) {
		this.compositionConfiguration = compositionConfiguration;
	}

	public static class Report {

		private String endpoint;

		public String getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}
	}

	public static class CompositionConfiguration {
		private String ra_composition_section_author;

		public String getCompositionSectionAuthor() {
			return ra_composition_section_author;
		}

		public void setRaCompositionSectionAuthor(String raCompositionSectionAuthor) {
			this.ra_composition_section_author = raCompositionSectionAuthor;
		}
	}
}
