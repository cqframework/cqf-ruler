package org.opencds.cqf.ruler.cr;

import org.opencds.cqf.ruler.builder.ResourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.cr")
public class CrProperties {
	private boolean enabled = true;
	private MeasureReportConfiguration measure_report;

	public boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public MeasureReportConfiguration getMeasureReport() {
		return this.measure_report;
	}

	public void setMeasureReport(MeasureReportConfiguration theMeasureReport) {
		this.measure_report = theMeasureReport;
	}

	public static class MeasureReportConfiguration {
		/**
		 * Implements the reporter element of the <a href=
		 * "https://www.hl7.org/fhir/measurereport.html">MeasureReport</a> FHIR
		 * Resource.
		 * This is required by the <a href=
		 * "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/indv-measurereport-deqm">DEQMIndividualMeasureReportProfile</a>
		 * profile found in the
		 * <a href="http://build.fhir.org/ig/HL7/davinci-deqm/index.html">Da Vinci DEQM
		 * FHIR Implementation Guide</a>.
		 **/
		private String reporter;

		public String getReporter() {
			return reporter;
		}

		public void setReporter(String reporter) {
			this.reporter = ResourceBuilder.ensureOrganizationReference(reporter);
		}
	}
}
