package org.opencds.cqf.ruler.utility;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;

public class BundleSettings extends ResourceSettings {

	private String type;
	private Date timestamp;

	@Override
	public BundleSettings withDefaults() {
		checkNotNull(this.type);
		super.withDefaults();

		if (this.timestamp == null) {
			this.timestamp = new Date();
		}

		return this;
	}

	public BundleSettings addType(String theType) {
		this.type = theType;

		return this;
	}

	public BundleSettings addTimestamp(Date theTimestamp) {
		this.timestamp = theTimestamp;

		return this;
	}

	public String type() {
		return this.type;
	}

	public Date timestamp() {
		return this.timestamp;
	}
}
