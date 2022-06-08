package org.opencds.cqf.ruler.cql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(CqlProperties.class)
public class CqlPropertiesTest {

	@Autowired
	CqlProperties cqlProperties;

	// This tests that all the various cql-related properties are being bound
	// correctly to the configuration
	@Test
	public void cqlPropertiesAreSet() {
		assertFalse(cqlProperties.isEnabled());
		assertFalse(cqlProperties.useEmbeddedLibraries());

		assertTrue(cqlProperties.getEngine().isDebugLoggingEnabled());
		assertTrue(cqlProperties.getEngine().getOptions().contains(CqlEngine.Options.EnableExpressionCaching));

		assertTrue(cqlProperties.getTranslator().getAnalyzeDataRequirements());
		assertTrue(cqlProperties.getTranslator().getCollapseDataRequirements());

		assertTrue(cqlProperties.getTranslator().getOptions().contains(CqlTranslator.Options.EnableResultTypes));
	}

}
