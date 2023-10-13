package org.opencds.cqf.ruler.cr.r4;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Configuration;
import org.hl7.fhir.r4.model.EnumFactory;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.PrimitiveType;

public class CRMIReleaseVersionBehavior {
  	public enum CRMIReleaseVersionBehaviorCodes {
		/**
		 * The version provided will be applied to the root artifact and all owned components if a version is not specified.
		 */
		DEFAULT,
		/**
		 * If the root artifact has a specified version different from the version passed to the operation, an error will be returned.
		 */
		CHECK,
		/**
		 * The version provided will be applied to the root artifact and all owned components, regardless of whether or not a version was already specified.
		 */
		FORCE,
		/**
		 * added to help the parsers with the generic types
		 */
		NULL;

		public static CRMIReleaseVersionBehaviorCodes fromCode(String codeString) throws FHIRException {
			if (codeString == null || "".equals(codeString))
				return null;
			if ("default".equals(codeString))
				return DEFAULT;
			if ("check".equals(codeString))
				return CHECK;
			if ("force".equals(codeString))
				return FORCE;
			if (Configuration.isAcceptInvalidEnums())
				return null;
			else
				throw new FHIRException("Unknown CRMIReleaseVersionBehaviorCodes '" + codeString + "'");
		}

		public String toCode() {
			switch (this) {
				case DEFAULT:
					return "default";
				case CHECK:
					return "check";
				case FORCE:
					return "force";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

		public String getSystem() {
			switch (this) {
				case DEFAULT:
					return "http://hl7.org/fhir/uv/crmi/ValueSet/crmi-release-version-behavior";
				case CHECK:
					return "http://hl7.org/fhir/uv/crmi/ValueSet/crmi-release-version-behavior";
				case FORCE:
					return "http://hl7.org/fhir/uv/crmi/ValueSet/crmi-release-version-behavior";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

		public String getDefinition() {
			switch (this) {
				case DEFAULT:
					return "The version provided will be applied to the root artifact and all owned components if a version is not specified.";
				case CHECK:
					return "If the root artifact has a specified version different from the version passed to the operation, an error will be returned.";
				case FORCE:
					return "The version provided will be applied to the root artifact and all owned components, regardless of whether or not a version was already specified.";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

		public String getDisplay() {
			switch (this) {
				case DEFAULT:
					return "Default";
				case CHECK:
					return "Check";
				case FORCE:
					return "Force";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

	}

	public static class CRMIReleaseVersionBehaviorCodesEnumFactory implements EnumFactory<CRMIReleaseVersionBehaviorCodes> {
		public CRMIReleaseVersionBehaviorCodes fromCode(String codeString) throws IllegalArgumentException {
			if (codeString == null || "".equals(codeString))
				if (codeString == null || "".equals(codeString))
					return null;
			if ("default".equals(codeString))
				return CRMIReleaseVersionBehaviorCodes.DEFAULT;
			if ("check".equals(codeString))
				return CRMIReleaseVersionBehaviorCodes.CHECK;
			if ("force".equals(codeString))
				return CRMIReleaseVersionBehaviorCodes.FORCE;
			throw new IllegalArgumentException("Unknown CRMIReleaseVersionBehaviorCodes code '" + codeString + "'");
		}

		public Enumeration<CRMIReleaseVersionBehaviorCodes> fromType(Base code) throws FHIRException {
			if (code == null)
				return null;
			if (code.isEmpty())
				return new Enumeration<CRMIReleaseVersionBehaviorCodes>(this);
			String codeString = ((PrimitiveType) code).asStringValue();
			if (codeString == null || "".equals(codeString))
				return null;
			if ("default".equals(codeString))
				return new Enumeration<CRMIReleaseVersionBehaviorCodes>(this, CRMIReleaseVersionBehaviorCodes.DEFAULT);
				if ("check".equals(codeString))
					return new Enumeration<CRMIReleaseVersionBehaviorCodes>(this, CRMIReleaseVersionBehaviorCodes.CHECK);
				if ("force".equals(codeString))
					return new Enumeration<CRMIReleaseVersionBehaviorCodes>(this, CRMIReleaseVersionBehaviorCodes.FORCE);
			throw new FHIRException("Unknown CRMIReleaseVersionBehaviorCodes code '" + codeString + "'");
		}

		public String toCode(CRMIReleaseVersionBehaviorCodes code) {
			if (code == CRMIReleaseVersionBehaviorCodes.DEFAULT)
				return "error";
			if (code == CRMIReleaseVersionBehaviorCodes.CHECK)
				return "check";
			if (code == CRMIReleaseVersionBehaviorCodes.FORCE)
				return "force";
			return "?";
		}

		public String toSystem(CRMIReleaseVersionBehaviorCodes code) {
			return code.getSystem();
		}
	}

}
