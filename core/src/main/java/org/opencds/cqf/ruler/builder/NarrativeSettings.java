package org.opencds.cqf.ruler.builder;

public class NarrativeSettings {

	private String text;
	private String status = "generated";

	public NarrativeSettings(String theText) {
		this.text = theText;
	}

	public NarrativeSettings(String theText, String theStatus) {
		this.text = theText;
		this.status = theStatus;
	}

	public String getText() {
		return this.text;
	}

	public String getStatus() {
		return this.status;
	}
}
