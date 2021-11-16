package org.opencds.cqf.ruler.plugin.hello;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "hello.world")
@Configuration
public class HelloWorldProperties {

    private String message = "Bye";

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
}
