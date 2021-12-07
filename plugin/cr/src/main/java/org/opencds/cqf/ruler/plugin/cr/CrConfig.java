package org.opencds.cqf.ruler.plugin.cr;

import java.util.Map;

import org.cqframework.cql.cql2elm.ModelManager;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.evaluator.cql2elm.model.CacheAwareModelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger ourLog = LoggerFactory.getLogger(CrConfig.class);
    
    @Bean
    public CrProperties crProperties() {
        return new CrProperties();
    }

    @Bean
    public ModelManager modelManager(
            Map<org.hl7.elm.r1.VersionedIdentifier, org.cqframework.cql.cql2elm.model.Model> globalModelCache) {
        return new CacheAwareModelManager(globalModelCache);
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
