package org.opencds.cqf.ruler.test;

import org.opencds.cqf.ruler.config.RulerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(RulerConfig.class)
public class DaoOnlyConfig {
	@Bean
	public TestDbService testDbService() {
		return new TestDbService();
	}

}
