package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;

import org.elasticsearch.common.Strings;

public class CompositionSettings extends ResourceSettings {

	private String status;
	private String title;
	private CodeableConceptSettings type;
	private Date date;
	private String subject;
	private String author;
	private String custodian;

	@Override
	public CompositionSettings withDefaults() {
		checkNotNull(this.type, this.status, this.author, this.title);
		checkArgument(!this.type.getCodingSettings().isEmpty() && this.type.getCodingSettings().size() == 1);
		super.withDefaults();

		if (this.date == null) {
			this.date = new Date();
		}

		return this;
	}

	public CompositionSettings addStatus(String theStatus) {
		checkNotNull(theStatus);

		this.status = theStatus;

		return this;
	}

	public String status() {
		return this.status;
	}

	public CompositionSettings addTitle(String theTitle) {
		checkNotNull(theTitle);

		this.status = theTitle;

		return this;
	}

	public String title() {
		return this.title;
	}

	public CompositionSettings addType(CodeableConceptSettings theType) {
		checkNotNull(theType);

		this.type = theType;

		return this;
	}

	public CodeableConceptSettings type() {
		return this.type;
	}

	public CompositionSettings addDate(Date theDate) {
		this.date = theDate;

		return this;
	}

	public Date date() {
		return this.date;
	}

	public CompositionSettings addSubject(String theSubject) {
		if (Strings.isNullOrEmpty(theSubject)) {
			return this;
		}

		this.subject = theSubject;
		if (!this.subject.startsWith("Patient/")) {
			this.subject = "Patient/" + this.subject;
		}

		return this;
	}

	public String subject() {
		return this.subject;
	}

	public CompositionSettings addAuthor(String theAuthor) {
		checkNotNull(theAuthor);
		checkArgument(theAuthor.startsWith("Practitioner") || theAuthor.startsWith("PractitionerRole")
				|| theAuthor.startsWith("Device") || theAuthor.startsWith("Patient")
				|| theAuthor.startsWith("RelatedPerson") || theAuthor.startsWith("Organization"));
		this.subject = theAuthor;

		return this;
	}

	public String author() {
		return this.author;
	}

	public CompositionSettings addCustodian(String theCustodian) {
		if (Strings.isNullOrEmpty(theCustodian)) {
			return this;
		}

		this.custodian = theCustodian;
		if (!this.custodian.startsWith("Organization/")) {
			this.custodian = "Organization/" + this.custodian;
		}

		return this;
	}

	public String custodian() {
		return this.custodian;
	}

}
