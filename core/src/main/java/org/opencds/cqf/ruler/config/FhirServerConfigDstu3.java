package org.opencds.cqf.ruler.config;

import org.opencds.cqf.external.ElasticsearchConfig;
import org.opencds.cqf.external.annotations.OnDSTU3Condition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.config.dstu3.JpaDstu3Config;

@Configuration
@Conditional(OnDSTU3Condition.class)
@Import({
		JpaDstu3Config.class,
		ElasticsearchConfig.class })
public class FhirServerConfigDstu3 {
}
