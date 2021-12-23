package org.opencds.cqf.ruler.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@ConditionalOnProperty(prefix = "spring", name = "debug", havingValue = "true", matchIfMissing = false)
public class BeanFinderConfig {
	
	@Bean
	BeanFinder beanFinder() {
		return new BeanFinder();
	}
	
}
