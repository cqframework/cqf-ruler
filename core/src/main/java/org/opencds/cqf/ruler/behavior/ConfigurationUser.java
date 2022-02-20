package org.opencds.cqf.ruler.behavior;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Set;

public interface ConfigurationUser {
	static final ConfigurationSet configurations = new ConfigurationSet();

	default void validateConfiguration(Object theConfiguration) {
		if (configurationValidated(theConfiguration)) {
			return;
		}
		checkNotNull(theConfiguration);
	}

	default void setConfigurationValid(Object theConfiguration) {
		configurations.getConfigurations().add(theConfiguration);
	}

	default void setConfigurationInvalid(Object theConfiguration) {
		configurations.getConfigurations().remove(theConfiguration);
	}

	default boolean configurationValidated(Object theConfiguration) {
		return configurations.getConfigurations().contains(theConfiguration);
	}

	class ConfigurationSet {
		private final Set<Object> configurations = new HashSet<>();

		public Set<Object> getConfigurations() {
			return this.configurations;
		}
	}
}
