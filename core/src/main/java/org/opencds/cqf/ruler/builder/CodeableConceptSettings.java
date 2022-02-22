package org.opencds.cqf.ruler.builder;

import java.util.HashSet;
import java.util.Set;

public class CodeableConceptSettings {

	private Set<CodingSettings> codingSettings = new HashSet<>();

	public CodeableConceptSettings add(String theSystem, String theCode) {
		add(theSystem, theCode, null);

		return this;
	}

	public CodeableConceptSettings add(String theSystem, String theCode, String theDisplay) {
		codingSettings.add(new CodingSettings(theSystem, theCode, theDisplay));

		return this;
	}

	public Set<CodingSettings> getCodingSettings() {
		return this.codingSettings;
	}

	public CodingSettings[] getCodingSettingsArray() {
		return getCodingSettings().toArray(new CodingSettings[0]);
	}

}
