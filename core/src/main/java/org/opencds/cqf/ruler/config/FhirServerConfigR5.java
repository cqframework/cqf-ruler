package org.opencds.cqf.ruler.config;


import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.config.r5.JpaR5Config;

@Configuration
@Conditional(org.opencds.cqf.jpa.starter.annotations.OnR5Condition.class)
@Import({
		JpaR5Config.class,
		org.opencds.cqf.jpa.starter.common.ElasticsearchConfig.class
})
public class FhirServerConfigR5 {
}
