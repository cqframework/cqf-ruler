package org.opencds.cqf.ruler.plugin.security;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.opencds.cqf.ruler.api.MetadataExtender;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {
    
    @Bean
	 @ConfigurationProperties(prefix = "hapi.fhir.security.oauth")
    public SecurityProperties OAuthProperties() {
        return new SecurityProperties();
    }

    @Bean
    @Conditional(OnR4Condition.class)
    public MetadataExtender<CapabilityStatement> OAuthProvider() { return new OAuthProvider(); }
}
