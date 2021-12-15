package org.opencds.cqf.ruler.plugin.security;

import org.opencds.cqf.ruler.api.MetadataExtender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.jpa.starter.annotations.OnDSTU3Condition;
import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

	@Bean
	public SecurityProperties OAuthProperties() {
		return new SecurityProperties();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public MetadataExtender<org.hl7.fhir.r4.model.CapabilityStatement> oAuthProviderR4() {
		return new org.opencds.cqf.ruler.plugin.security.r4.OAuthProvider();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public MetadataExtender<org.hl7.fhir.dstu3.model.CapabilityStatement> oAuthProviderDstu3() {
		return new org.opencds.cqf.ruler.plugin.security.dstu3.OAuthProvider();
	}
}
