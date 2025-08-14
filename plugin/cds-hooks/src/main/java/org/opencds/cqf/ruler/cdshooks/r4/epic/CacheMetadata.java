package org.opencds.cqf.ruler.cdshooks.r4.epic;

import org.opencds.cqf.ruler.cdshooks.r4.epic.util.CdsHooksRequestHelper;

import java.util.List;

public class CacheMetadata {

	private CdsHooksRequestHelper request;
	private String response;
	private Long startTime;

	public CacheMetadata() {}

	public CacheMetadata(String response, Long startTime) {
		this.response = response;
		this.startTime = startTime;
	}

	public CacheMetadata(CdsHooksRequestHelper request, String response, Long startTime) {
		this.request = request;
		this.response = response;
		this.startTime = startTime;
	}

	public CdsHooksRequestHelper getRequest() {
		return request;
	}

	public void setRequest(CdsHooksRequestHelper request) {
		this.request = request;
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
