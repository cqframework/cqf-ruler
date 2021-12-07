package org.opencds.cqf.ruler.plugin.cpg;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.starter.annotations.OnDSTU2Condition;
import ca.uhn.fhir.jpa.starter.annotations.OnDSTU3Condition;
import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import ca.uhn.fhir.jpa.starter.annotations.OnR5Condition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.ModelManager;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.fhir.model.Dstu2FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.cql2elm.model.CacheAwareModelManager;
import org.opencds.cqf.cql.evaluator.engine.model.CachingModelResolverDecorator;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.cpg", name = "enabled", havingValue = "true")
public class CpgConfig {
    private static final Logger ourLog = LoggerFactory.getLogger(CpgConfig.class);

	@Bean
	public CpgProperties cpgProperties() {
		return new CpgProperties();
	}

    @Bean
    public CqlTranslatorOptions cqlTranslatorOptions(FhirContext fhirContext) {
        // TODO: get from properties
        CqlTranslatorOptions options = CqlTranslatorOptions.defaultOptions();

        if (fhirContext.getVersion().getVersion().isOlderThan(FhirVersionEnum.R4)
                && (options.getCompatibilityLevel() == "1.5" || options.getCompatibilityLevel() == "1.4")) {
            ourLog.warn(
                    "This server is configured to use CQL version > 1.4 and FHIR version <= DSTU3. Most available CQL content for DSTU3 and below is for CQL versions 1.3 or 1.4. If your CQL content causes translation errors, try setting the CQL compatibility level to 1.3");
        }

        return options;
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

    // TODO: Use something like caffeine caching for this so that growth is limited.
    @Bean
    public Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> globalLibraryCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Map<org.hl7.elm.r1.VersionedIdentifier, org.cqframework.cql.cql2elm.model.Model> globalModelCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    @Conditional(OnDSTU3Condition.class) 
    public OperationProvider dstu3CqlExecutionProvider() {
        return new org.opencds.cqf.ruler.plugin.cpg.dstu3.provider.CqlExecutionProvider();
    }

    @Bean
    @Conditional(OnR4Condition.class) 
    public OperationProvider r4CqlExecutionProvider() {
        return new org.opencds.cqf.ruler.plugin.cpg.r4.provider.CqlExecutionProvider();
    }

}
