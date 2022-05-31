package org.opencds.cqf.ruler.security;

import org.opencds.cqf.ruler.api.MetadataExtender;
import org.opencds.cqf.ruler.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.ruler.external.annotations.OnR4Condition;
import org.opencds.cqf.ruler.security.interceptor.BasicAuthenticationInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

	@Bean
	public SecurityProperties OAuthProperties() {
		return new SecurityProperties();
	}

	@Bean
	public BasicAuthenticationInterceptor basicAuthenticationInterceptor() {
		return new BasicAuthenticationInterceptor();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public MetadataExtender<org.hl7.fhir.r4.model.CapabilityStatement> oAuthProviderR4() {
		return new org.opencds.cqf.ruler.security.r4.OAuthProvider();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	public MetadataExtender<org.hl7.fhir.dstu3.model.CapabilityStatement> oAuthProviderDstu3() {
		return new org.opencds.cqf.ruler.security.dstu3.OAuthProvider();
	}
}
