package org.opencds.cqf.ruler.builder;

public class CodingSettings {

	private String system;
	private String code;
	private String display;

	public CodingSettings(String theSystem, String theCode) {
		this(theSystem, theCode, null);
	}

	public CodingSettings(String theSystem, String theCode, String theDisplay) {
		this.system = theSystem;
		this.code = theCode;
		this.display = theDisplay;
	}

	public String getSystem() {
		return this.system;
	}

	public String getCode() {
		return this.code;
	}

	public String getDisplay() {
		return this.display;
	}

}
