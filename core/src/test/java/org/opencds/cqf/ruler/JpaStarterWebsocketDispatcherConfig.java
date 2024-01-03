package org.opencds.cqf.ruler;

import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class ensures that websockets work with
 * Spring + Spring Boot + Jetty
 */
@Configuration
public class JpaStarterWebsocketDispatcherConfig {

	@Bean
	public Jetty10WebSocketServletWebServerCustomizer jetty10WebSocketServletWebServerCustomizer() {
		return new Jetty10WebSocketServletWebServerCustomizer();
	}

	static class Jetty10WebSocketServletWebServerCustomizer implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

		@Override
		public void customize(JettyServletWebServerFactory factory) {

			factory.addServerCustomizers(server -> {
				WebAppContext ctx = (WebAppContext) server.getHandler();
				JakartaWebSocketServletContainerInitializer.configure(ctx, null);
			});

		}
	}
}
