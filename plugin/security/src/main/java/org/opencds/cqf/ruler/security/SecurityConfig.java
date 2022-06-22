package org.opencds.cqf.ruler.security;

import org.opencds.cqf.ruler.api.MetadataExtender;
import org.opencds.cqf.ruler.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.ruler.external.annotations.OnR4Condition;
import org.opencds.cqf.ruler.security.interceptor.AuthenticationInterceptor;
import org.opencds.cqf.ruler.security.interceptor.RulerExceptionHandlingInterceptor;
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
	public RulerExceptionHandlingInterceptor rulerExceptionHandlingInterceptor() {
		return new RulerExceptionHandlingInterceptor();
	}

	@Bean
	@ConditionalOnProperty(prefix = "hapi.fhir.security.basic_auth", name = "enabled", havingValue = "true", matchIfMissing = false)
	public AuthenticationInterceptor authenticationInterceptor() {
		return new AuthenticationInterceptor();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	@ConditionalOnProperty(prefix = "hapi.fhir.security.oauth", name = "enabled", havingValue = "true")
	public MetadataExtender<org.hl7.fhir.r4.model.CapabilityStatement> oAuthProviderR4() {
		return new org.opencds.cqf.ruler.security.r4.OAuthProvider();
	}

	@Bean
	@Conditional(OnDSTU3Condition.class)
	@ConditionalOnProperty(prefix = "hapi.fhir.security.oauth", name = "enabled", havingValue = "true")
	public MetadataExtender<org.hl7.fhir.dstu3.model.CapabilityStatement> oAuthProviderDstu3() {
		return new org.opencds.cqf.ruler.security.dstu3.OAuthProvider();
	}
}
