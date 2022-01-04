package org.opencds.cqf.ruler.sdc;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.external.annotations.OnDSTU3Condition;
import org.opencds.cqf.ruler.external.annotations.OnR4Condition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.sdc", name ="enabled", havingValue = "true", matchIfMissing = true)
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
}
