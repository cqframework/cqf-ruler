package org.hl7.davinci.atr.server;

import org.hl7.davinci.atr.server.service.MALService;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.external.annotations.OnR4Condition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;


/**
 * The Class MALConfig.
 */
@Configuration
public class MALConfig {

	/**
	 * Member add provider.
	 *
	 * @return the operation provider
	 */
	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider memberAddProvider() {
		return new MALProvider();
	}
	
	/**
	 * Mal service.
	 *
	 * @return the MAL service
	 */
	@Bean
	public MALService malService() {
		return new MALService();
	}
}
