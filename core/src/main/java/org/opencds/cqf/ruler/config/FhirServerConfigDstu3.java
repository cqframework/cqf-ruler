package org.opencds.cqf.ruler.config;


import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.config.dstu3.JpaDstu3Config;

@Configuration
@Conditional(org.opencds.cqf.jpa.starter.annotations.OnDSTU3Condition.class)
@Import({
		JpaDstu3Config.class,
		org.opencds.cqf.jpa.starter.common.ElasticsearchConfig.class })
public class FhirServerConfigDstu3 {
}
