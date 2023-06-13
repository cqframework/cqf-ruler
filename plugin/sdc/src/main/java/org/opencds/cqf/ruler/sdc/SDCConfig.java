package org.opencds.cqf.ruler.sdc;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import org.opencds.cqf.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.external.annotations.OnR4Condition;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.sdc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SDCConfig {

    @Bean
    public SDCProperties SDCProperties() {
        return new SDCProperties();
    }

    @Bean
    @Conditional(OnR4Condition.class)
    public OperationProvider r4ExtractProvider() {
        return new org.opencds.cqf.ruler.sdc.r4.ExtractProvider();
    }

    @Bean
    @Conditional(OnDSTU3Condition.class)
    public OperationProvider dstu3ExtractProvider() {
        return new org.opencds.cqf.ruler.sdc.dstu3.ExtractProvider();
    }

    @Bean
    @Conditional(OnR4Condition.class)
    public OperationProvider r4TransformProvider() {
        return new org.opencds.cqf.ruler.sdc.r4.TransformProvider();
    }

    @Bean
    @Conditional(OnDSTU3Condition.class)
    public OperationProvider dstu3TransformProvider() {
        return new org.opencds.cqf.ruler.sdc.dstu3.TransformProvider();
    }
	@Bean
	SDCProviderFactory sdcOperationFactory() {
		return new SDCProviderFactory();
	}

	@Bean
	SDCProviderLoader sdcProviderLoader(FhirContext theFhirContext, ResourceProviderFactory theResourceProviderFactory,
													SDCProviderFactory theSDCProviderFactory) {
		return new SDCProviderLoader(theFhirContext, theResourceProviderFactory, theSDCProviderFactory);
	}
}
