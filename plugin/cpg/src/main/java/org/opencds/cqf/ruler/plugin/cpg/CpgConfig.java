package org.opencds.cqf.ruler.plugin.cpg;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.external.annotations.OnR4Condition;
import org.opencds.cqf.ruler.plugin.cql.CqlConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.cpg", name ="enabled", havingValue = "true", matchIfMissing=true)
@Import({CqlConfig.class})
public class CpgConfig {
	@Bean
	public CpgProperties cpgProperties() {
		return new CpgProperties();
	}


	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider r4LibraryEvaluationProvider() {
		return new org.opencds.cqf.ruler.plugin.cpg.r4.provider.LibraryEvaluationProvider();
	}

}
