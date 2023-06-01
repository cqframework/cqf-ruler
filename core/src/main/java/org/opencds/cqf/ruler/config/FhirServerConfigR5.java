package org.opencds.cqf.ruler.config;


import ca.uhn.fhir.jpa.topic.SubscriptionTopicConfig;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.config.r5.JpaR5Config;

@Configuration
@Conditional(org.opencds.cqf.jpa.starter.annotations.OnR5Condition.class)
@Import({
		org.opencds.cqf.jpa.starter.common.StarterJpaConfig.class,
	JpaR5Config.class,
	SubscriptionTopicConfig.class,
	org.opencds.cqf.jpa.starter.common.ElasticsearchConfig.class
})
public class FhirServerConfigR5 {
}
