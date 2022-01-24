package org.opencds.cqf.ruler.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("org.opencds.cqf.ruler.external")
public class DaoOnlyConfig {

	@Bean
	public TestDbService testDbService() {
		return new TestDbService();
	}

}
