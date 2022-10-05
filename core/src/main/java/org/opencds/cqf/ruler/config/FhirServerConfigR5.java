package org.opencds.cqf.ruler.config;

import org.opencds.cqf.external.ElasticsearchConfig;
import org.opencds.cqf.external.annotations.OnR5Condition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.config.r5.JpaR5Config;

@Configuration
@Conditional(OnR5Condition.class)
@Import({
		JpaR5Config.class,
		ElasticsearchConfig.class
})
public class FhirServerConfigR5 {
}
