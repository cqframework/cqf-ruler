package org.opencds.cqf.ruler.cql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CqlPropertiesFactory {
	@Bean
	CqlProperties cqlProperties() {
		return new CqlProperties();
	}
}
