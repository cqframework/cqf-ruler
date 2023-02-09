package org.opencds.cqf.ruler.cql.r4;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Configuration;
import org.hl7.fhir.r4.model.EnumFactory;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;

public class ArtifactCommentExtension extends Extension {
	public enum ArtifactCommentType {
		/**
		 * The comment is providing additional documentation from an authoring
		 * perspective
		 */
		DOCUMENTATION,
		/**
		 * The comment is providing feedback from a reviewer and requires resolution
		 */
		REVIEW,
		/**
		 * The comment is providing usage guidance to an artifact consumer
		 */
		GUIDANCE,
		/**
		 * added to help the parsers with the generic types
		 */
		NULL;

		public static ArtifactCommentType fromCode(String codeString) throws FHIRException {
			if (codeString == null || "".equals(codeString))
				return null;
			if ("documentation".equals(codeString))
				return DOCUMENTATION;
			if ("review".equals(codeString))
				return REVIEW;
			if ("guidance".equals(codeString))
				return GUIDANCE;
			if (Configuration.isAcceptInvalidEnums())
				return null;
			else
				throw new FHIRException("Unknown ArtifactCommentType '" + codeString + "'");
		}

		public String toCode() {
			switch (this) {
				case DOCUMENTATION:
					return "documentation";
				case REVIEW:
					return "review";
				case GUIDANCE:
					return "guidance";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

		public String getSystem() {
			switch (this) {
				case DOCUMENTATION:
					return "http://hl7.org/fhir/us/cqfmeasures/CodeSystem/artifact-comment-type";
				case REVIEW:
					return "http://hl7.org/fhir/us/cqfmeasures/CodeSystem/artifact-comment-type";
				case GUIDANCE:
					return "http://hl7.org/fhir/us/cqfmeasures/CodeSystem/artifact-comment-type";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

		public String getDefinition() {
			switch (this) {
				case DOCUMENTATION:
					return "The comment is providing additional documentation from an authoring perspective.";
				case REVIEW:
					return "The comment is providing usage guidance to an artifact consumer.";
				case GUIDANCE:
					return "The comment is providing usage guidance to an artifact consumer.";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

		public String getDisplay() {
			switch (this) {
				case DOCUMENTATION:
					return "Documentation";
				case REVIEW:
					return "Review";
				case GUIDANCE:
					return "Guidance";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

	}

	public static class ArtifactCommentTypeEnumFactory implements EnumFactory<ArtifactCommentType> {
		public ArtifactCommentType fromCode(String codeString) throws IllegalArgumentException {
			if (codeString == null || "".equals(codeString))
				if (codeString == null || "".equals(codeString))
					return null;
			if ("documentation".equals(codeString))
				return ArtifactCommentType.DOCUMENTATION;
			if ("guidance".equals(codeString))
				return ArtifactCommentType.GUIDANCE;
			if ("review".equals(codeString))
				return ArtifactCommentType.REVIEW;
			throw new IllegalArgumentException("Unknown ArtifactCommentType code '" + codeString + "'");
		}

		public Enumeration<ArtifactCommentType> fromType(Base code) throws FHIRException {
			if (code == null)
				return null;
			if (code.isEmpty())
				return new Enumeration<ArtifactCommentType>(this);
			String codeString = ((PrimitiveType) code).asStringValue();
			if (codeString == null || "".equals(codeString))
				return null;
			if ("documentation".equals(codeString))
				return new Enumeration<ArtifactCommentType>(this, ArtifactCommentType.DOCUMENTATION);
			if ("guidance".equals(codeString))
				return new Enumeration<ArtifactCommentType>(this, ArtifactCommentType.GUIDANCE);
			if ("review".equals(codeString))
				return new Enumeration<ArtifactCommentType>(this, ArtifactCommentType.REVIEW);

			throw new FHIRException("Unknown ArtifactCommentType code '" + codeString + "'");
		}

		public String toCode(ArtifactCommentType code) {
			if (code == ArtifactCommentType.DOCUMENTATION)
				return "documentation";
			if (code == ArtifactCommentType.GUIDANCE)
				return "guidance";
			if (code == ArtifactCommentType.REVIEW)
				return "review";
			return "?";
		}

		public String toSystem(ArtifactCommentType code) {
			return code.getSystem();
		}
	}

	public ArtifactCommentExtension() {
		super("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-artifactComment");
	}

	public ArtifactCommentExtension setTypeExtension(String type) {
		if (type != null) {
			this.typeExtension = new ArtifactCommentTypeExtension(type);
		}
		return this;
	}

	public ArtifactCommentExtension setTextExtension(String type) {
		if (type != null) {
			this.textExtension = new ArtifactCommentTextExtension();
			this.textExtension.setValue(new MarkdownType(type));
		}
		return this;
	}

	public ArtifactCommentExtension setTargetExtension(String type) {
		if (type != null) {
			this.targetExtension = new ArtifactCommentTargetExtension();
			this.targetExtension.setValue(new UriType(type));
		}
		return this;
	}

	public ArtifactCommentExtension setReferenceExtension(String type) {
		if (type != null) {
			this.referenceExtension = new ArtifactCommentReferenceExtension();
			this.referenceExtension.setValue(new UriType(type));
		}
		return this;
	}

	public ArtifactCommentExtension setUserExtension(String type) {
		if (type != null) {
			this.userExtension = new ArtifactCommentUserExtension();
			this.userExtension.setValue(new StringType(type));
		}
		return this;
	}

	protected ArtifactCommentTypeExtension typeExtension;
	protected ArtifactCommentTextExtension textExtension;
	protected ArtifactCommentTargetExtension targetExtension;
	protected ArtifactCommentReferenceExtension referenceExtension;
	protected ArtifactCommentUserExtension userExtension;

	private class ArtifactCommentTypeExtension extends Extension {
		Enumeration<ArtifactCommentType> typeCode = new Enumeration<ArtifactCommentType>(
				new ArtifactCommentTypeEnumFactory());

		public ArtifactCommentTypeExtension(String type) throws FHIRException {
			super("type");
			typeCode.setValue(ArtifactCommentType.fromCode(type));
			this.setValue(typeCode);
		}

	}

	private class ArtifactCommentTextExtension extends Extension {
		public ArtifactCommentTextExtension() {
			super("text");
		}
	}

	private class ArtifactCommentTargetExtension extends Extension {
		public ArtifactCommentTargetExtension() {
			super("target");
		}
	}

	private class ArtifactCommentReferenceExtension extends Extension {
		public ArtifactCommentReferenceExtension() {
			super("reference");
		}
	}

	private class ArtifactCommentUserExtension extends Extension {
		public ArtifactCommentUserExtension() {
			super("user");
		}
	}
}
