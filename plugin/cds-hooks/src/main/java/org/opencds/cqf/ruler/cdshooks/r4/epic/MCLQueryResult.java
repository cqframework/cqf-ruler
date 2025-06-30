package org.opencds.cqf.ruler.cdshooks.r4.epic;

public class MCLQueryResult {
	private Long duration;
	private Integer count;
	private Long bytes;

	public MCLQueryResult() {}

	public MCLQueryResult(Long duration, Integer count, Long bytes) {
		this.duration = duration;
		this.count = count;
		this.bytes = bytes;
	}

	public Long getDuration() {
		return duration;
	}

	public Integer getCount() {
		return count;
	}

	public Long getBytes() {
		return bytes;
	}

	public void setBytes(Long bytes) {
		this.bytes = bytes;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}
}
