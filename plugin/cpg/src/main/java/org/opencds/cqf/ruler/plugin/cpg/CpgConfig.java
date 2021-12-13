package org.opencds.cqf.ruler.plugin.cpg;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.plugin.cpg.helpers.common.LoggingHelper;
import org.opencds.cqf.ruler.plugin.cpg.helpers.r4.LibraryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.cpg", name ="enabled", havingValue = "true")
public class CpgConfig {
    private static final Logger ourLog = LoggerFactory.getLogger(CpgConfig.class);

	@Bean
	public CpgProperties cpgProperties() {
		return new CpgProperties();
	}


	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4LibraryEvaluationProvider() {
		return new org.opencds.cqf.ruler.plugin.cpg.r4.provider.LibraryEvaluationProvider();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public LibraryHelper r4LibraryHelper(Map<VersionedIdentifier, Model> modelCache,
													 Map<org.cqframework.cql.elm.execution.VersionedIdentifier, Library> libraryCache,
													 CqlTranslatorOptions translatorOptions) {
		return new org.opencds.cqf.ruler.plugin.cpg.helpers.r4.LibraryHelper(modelCache, libraryCache, translatorOptions);
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public LoggingHelper r4LoggingHelper() {
		return new org.opencds.cqf.ruler.plugin.cpg.helpers.common.LoggingHelper();
	}

}
