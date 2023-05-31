package org.opencds.cqf.ruler.security;



import org.opencds.cqf.ruler.api.MetadataExtender;
import org.opencds.cqf.ruler.security.interceptor.AuthenticationInterceptor;
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
	@ConditionalOnProperty(prefix = "hapi.fhir.security.basic_auth", name = "enabled", havingValue = "true", matchIfMissing = false)
	public AuthenticationInterceptor authenticationInterceptor() {
		return new AuthenticationInterceptor();
	}

	@Bean
	@Conditional(org.opencds.cqf.jpa.starter.annotations.OnR4Condition.class)
	@ConditionalOnProperty(prefix = "hapi.fhir.security.oauth", name = "enabled", havingValue = "true")
	public MetadataExtender<org.hl7.fhir.r4.model.CapabilityStatement> oAuthProviderR4() {
		return new org.opencds.cqf.ruler.security.r4.OAuthProvider();
	}

	@Bean
	@Conditional(org.opencds.cqf.jpa.starter.annotations.OnDSTU3Condition.class)
	@ConditionalOnProperty(prefix = "hapi.fhir.security.oauth", name = "enabled", havingValue = "true")
	public MetadataExtender<org.hl7.fhir.dstu3.model.CapabilityStatement> oAuthProviderDstu3() {
		return new org.opencds.cqf.ruler.security.dstu3.OAuthProvider();
	}
}
