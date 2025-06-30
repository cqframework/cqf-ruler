package org.opencds.cqf.ruler.cdshooks.r4.epic;

import java.util.List;

public class CacheMetadata {

	private String response;
	private Long startTime;

	public CacheMetadata() {}

	public CacheMetadata(String response, Long startTime) {
		this.response = response;
		this.startTime = startTime;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}
}
