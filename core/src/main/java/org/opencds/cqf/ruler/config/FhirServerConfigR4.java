package org.opencds.cqf.ruler.config;

import org.opencds.cqf.external.ElasticsearchConfig;
import org.opencds.cqf.external.annotations.OnR4Condition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.config.r4.JpaR4Config;

@Configuration
@Conditional(OnR4Condition.class)
@Import({
		JpaR4Config.class,
		ElasticsearchConfig.class
})
public class FhirServerConfigR4 {
}
