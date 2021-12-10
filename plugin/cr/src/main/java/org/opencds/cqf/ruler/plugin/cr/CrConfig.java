package org.opencds.cqf.ruler.plugin.cr;

import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.annotations.OnDSTU3Condition;
import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.cr", name = "enabled", havingValue = "true")
public class CrConfig {
    @Bean
    public CrProperties crProperties() {
        return new CrProperties();
    }

    @Bean
    SearchParameterResolver searchParameterResolver(FhirContext fhirContext) {
        return new SearchParameterResolver(fhirContext);
    }

    @Bean
    @Conditional(OnDSTU3Condition.class)
    public org.opencds.cqf.ruler.plugin.cr.dstu3.provider.ActivityDefinitionApplyProvider dstu3ActivityDefinitionApplyProvider() {
        return new org.opencds.cqf.ruler.plugin.cr.dstu3.provider.ActivityDefinitionApplyProvider();
    }

    @Bean
    @Conditional(OnR4Condition.class)
    public org.opencds.cqf.ruler.plugin.cr.r4.provider.ActivityDefinitionApplyProvider r4ActivityDefinitionApplyProvider() {
        return new org.opencds.cqf.ruler.plugin.cr.r4.provider.ActivityDefinitionApplyProvider();
    }

    @Bean
    @Conditional(OnDSTU3Condition.class)
    public org.opencds.cqf.ruler.plugin.cr.dstu3.ExpressionEvaluation dstu3ExpressionEvaluation() {
        return new org.opencds.cqf.ruler.plugin.cr.dstu3.ExpressionEvaluation();
    }

    @Bean
    @Conditional(OnR4Condition.class)
    public org.opencds.cqf.ruler.plugin.cr.r4.ExpressionEvaluation r4ExpressionEvaluation() {
        return new org.opencds.cqf.ruler.plugin.cr.r4.ExpressionEvaluation();
    }
}
