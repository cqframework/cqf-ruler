package org.opencds.cqf.ruler.cql.r4;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Configuration;
import org.hl7.fhir.r4.model.EnumFactory;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;

import ca.uhn.fhir.model.api.annotation.DatatypeDef;

@DatatypeDef(name="ArtifactCommentExtension", isSpecialization = true, profileOf = Extension.class)
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
					return "The comment is providing feedback from a reviewer and requires resolution.";
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
	
	public static final String ARTIFACT_COMMENT_EXTENSION_URL = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-artifactComment";
	public static final String TYPE = "type";
	public static final String TEXT = "text";
	public static final String TARGET = "target";
	public static final String REFERENCE = "reference";
	public static final String USER = "user";

	public ArtifactCommentExtension(String type, String text, String target, String reference, String user) throws FHIRException {
		super(ARTIFACT_COMMENT_EXTENSION_URL);
		setTypeExtension(type);
		setTextExtension(text);
		setTargetExtension(target);
		setReferenceExtension(reference);
		setUserExtension(user);
	}

	public ArtifactCommentExtension setTypeExtension(String type) throws FHIRException {
		if (type != null) {
			int index = findIndex(TYPE, this.getExtension());
			if(index != -1){
				this.extension.set(index, new ArtifactCommentTypeExtension(type));
			} else {
				this.addExtension(new ArtifactCommentTypeExtension(type));
			}
		}
		return this;
	}

	public ArtifactCommentExtension setTextExtension(String text) {
		if (text != null) {
			int index = findIndex(TEXT, this.getExtension());
			if(index != -1){
				this.extension.set(index, new ArtifactCommentTextExtension(text));
			} else {
				this.addExtension(new ArtifactCommentTextExtension(text));
			}
		}
		return this;
	}

	public ArtifactCommentExtension setTargetExtension(String target) {
		if (target != null) {
			int index = findIndex(TARGET, this.getExtension());
			if(index != -1){
				this.extension.set(index, new ArtifactCommentTargetExtension(target));
			} else {
				this.addExtension(new ArtifactCommentTargetExtension(target));
			}
		}
		return this;
	}

	public ArtifactCommentExtension setReferenceExtension(String reference) {
		if (reference != null) {
			int index = findIndex(REFERENCE, this.getExtension());
			if(index != -1){
				this.extension.set(index, new ArtifactCommentReferenceExtension(reference));
			} else {
				this.addExtension(new ArtifactCommentReferenceExtension(reference));
			}
		}
		return this;
	}

	public ArtifactCommentExtension setUserExtension(String user) {
		if (user != null) {
			int index = findIndex(USER, this.getExtension());
			if(index != -1){
				this.extension.set(index, new ArtifactCommentUserExtension(user));
			} else {
				this.addExtension(new ArtifactCommentUserExtension(user));
			}
		}
		return this;
	}

	private int findIndex(String url, List<Extension> extensions){
		Optional<Extension> existingExtension =  extensions.stream()
			.filter(e -> e.getUrl().equals(url)).findAny();
			if(existingExtension.isPresent()){
				return extensions.indexOf(existingExtension.get());
			} else {
				return -1;
			}
	}

	@DatatypeDef(name="ArtifactCommentTypeExtension", isSpecialization = true, profileOf = Extension.class)
	
	private class ArtifactCommentTypeExtension extends Extension {
		Enumeration<ArtifactCommentType> typeCode = new Enumeration<ArtifactCommentType>(
				new ArtifactCommentTypeEnumFactory());

		public ArtifactCommentTypeExtension(String type) throws FHIRException {
			super(TYPE);
			typeCode.setValue(ArtifactCommentType.fromCode(type));
			this.setValue(typeCode);
		}
	}

	@DatatypeDef(name="ArtifactCommentTextExtension", isSpecialization = true, profileOf = Extension.class)

	private class ArtifactCommentTextExtension extends Extension {
		public ArtifactCommentTextExtension(String text) {
			super(TEXT, new StringType(text));
		}
	}

	@DatatypeDef(name="ArtifactCommentTargetExtension", isSpecialization = true, profileOf = Extension.class)

	private class ArtifactCommentTargetExtension extends Extension {
		public ArtifactCommentTargetExtension(String target) {
			super(TARGET, new StringType(target));
		}
	}

	@DatatypeDef(name="ArtifactCommentReferenceExtension", isSpecialization = true, profileOf = Extension.class)

	private class ArtifactCommentReferenceExtension extends Extension {
		public ArtifactCommentReferenceExtension(String reference) throws FHIRException {
			super(REFERENCE);
			// https://hl7.org/fhir/R4/datatypes.html#uri
			Pattern uriPattern = Pattern.compile("\\S*");
			if(!uriPattern.matcher(reference).matches()){
				throw new FHIRException("artifactCommentReference is not a valid URI type");
			}
			this.setValue(new UriType(reference));
		}
	}
	@DatatypeDef(name="ArtifactCommentUserExtension", isSpecialization = true, profileOf = Extension.class)

	private class ArtifactCommentUserExtension extends Extension {
		public ArtifactCommentUserExtension(String user) {
			super(USER, new StringType(user));
		}
	}
}
