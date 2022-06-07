package org.opencds.cqf.ruler.atr;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.atr.service.MalService;
import org.opencds.cqf.ruler.external.annotations.OnR4Condition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;


/**
 * The Class MALConfig.
 */
@Configuration
public class AtrConfig {

	/**
	 * Member add provider.
	 *
	 * @return the operation provider
	 */
	@Bean
	@Conditional(OnR4Condition.class)
	public OperationProvider memberAddProvider() {
		return new MalProvider();
	}

	/**
	 * Mal service.
	 *
	 * @return the MAL service
	 */
	@Bean
	@Conditional(OnR4Condition.class)
	public MalService malService() {
		return new MalService();
	}
}
