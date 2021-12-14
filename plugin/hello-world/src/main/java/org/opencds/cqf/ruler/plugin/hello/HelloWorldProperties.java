package org.opencds.cqf.ruler.plugin.hello;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hello.world")
public class HelloWorldProperties {

	private String message = "Bye";

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
