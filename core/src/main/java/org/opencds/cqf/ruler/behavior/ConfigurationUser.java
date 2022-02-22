package org.opencds.cqf.ruler.behavior;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Set;

public interface ConfigurationUser {
	static final ConfigurationSet configurations = new ConfigurationSet();

	public abstract void validateConfiguration();

	default ConfigurationUser validateConfiguration(Object theConfiguration, boolean theExpression, String theMessage) {
		if (configurationValid(theConfiguration)) {
			return this;
		}

		try {
			checkNotNull(theConfiguration);
			checkArgument(theExpression, theMessage);
		} catch (Exception e) {
			setConfigurationInvalid(theConfiguration);
			throw e;
		}

		return this;
	}

	default void setConfigurationValid(Object theConfiguration) {
		configurations.getConfigurations().add(theConfiguration);
	}

	default void setConfigurationInvalid(Object theConfiguration) {
		configurations.getConfigurations().remove(theConfiguration);
	}

	default boolean configurationValid(Object theConfiguration) {
		return configurations.getConfigurations().contains(theConfiguration);
	}

	class ConfigurationSet {
		private final Set<Object> configurations = new HashSet<>();

		public Set<Object> getConfigurations() {
			return this.configurations;
		}
	}
}
