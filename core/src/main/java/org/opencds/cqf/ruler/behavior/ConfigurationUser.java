package org.opencds.cqf.ruler.behavior;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashSet;
import java.util.Set;

import ca.uhn.fhir.rest.api.server.RequestDetails;

public interface ConfigurationUser {
	static final ConfigurationSet configurations = new ConfigurationSet();

	public abstract void validateConfiguration(RequestDetails theRequestDetails);

	default <T> ConfigurationUser validateConfiguration(Class<T> theClass, boolean theExpression, String theMessage) {
		if (configurationValid(theClass)) {
			return this;
		}

		try {
			checkArgument(theExpression, theMessage);
		} catch (Exception e) {
			setConfigurationInvalid(theClass);
			throw e;
		}

		return this;
	}

	default <T> void setConfigurationValid(Class<T> theClass) {
		configurations.getConfigurations().add(theClass.getName());
	}

	default <T> void setConfigurationInvalid(Class<T> theClass) {
		configurations.getConfigurations().remove(theClass.getName());
	}

	default <T> boolean configurationValid(Class<T> theClass) {
		return configurations.getConfigurations().contains(theClass.getName());
	}

	class ConfigurationSet {
		private final Set<String> configurations = new HashSet<>();

		public Set<String> getConfigurations() {
			return this.configurations;
		}
	}
}
