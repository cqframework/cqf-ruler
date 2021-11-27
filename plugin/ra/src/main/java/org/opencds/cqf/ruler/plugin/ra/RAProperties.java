package org.opencds.cqf.ruler.plugin.ra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "hapi.fhir.ra")
@Configuration
@EnableConfigurationProperties
public class RAProperties {

    private Boolean enabled = true;

    public Boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    private Report report = new Report();

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public static class Report {

    }
}
