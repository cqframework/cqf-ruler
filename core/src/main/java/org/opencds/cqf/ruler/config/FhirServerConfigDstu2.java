package org.opencds.cqf.ruler.config;

import org.opencds.cqf.external.annotations.OnDSTU2Condition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.config.JpaDstu2Config;

@Configuration
@Conditional(OnDSTU2Condition.class)
@Import({
		JpaDstu2Config.class
})
public class FhirServerConfigDstu2 {
}
