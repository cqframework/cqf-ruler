package org.opencds.cqf.ruler.behavior;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;

public interface ConfigurationUser {
	static final HashSet<Object> configurations = new HashSet<>();

	default void validateConfiguration(Object theConfiguration) {
		if (validated(theConfiguration)) {
			return;
		}
		checkNotNull(theConfiguration);
		configurations.add(theConfiguration);
	}

	default boolean validated(Object theConfiguration) {
		return configurations.contains(theConfiguration);
	}
}
