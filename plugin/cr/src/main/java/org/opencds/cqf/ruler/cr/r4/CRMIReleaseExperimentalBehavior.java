package org.opencds.cqf.ruler.cr.r4;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Configuration;
import org.hl7.fhir.r4.model.EnumFactory;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.PrimitiveType;

public class CRMIReleaseExperimentalBehavior {
  	public enum CRMIReleaseExperimentalBehaviorCodes {
		/**
		 * The repository should throw an error if a specification which is not Experimental references Experimental components.
		 */
		ERROR,
		/**
		 * The repository should warn if a specification which is not Experimental references Experimental components.
		 */
		WARN,
		/**
		 * The repository does not need to consider the state of Experimental.
		 */
		NONE,
		/**
		 * added to help the parsers with the generic types
		 */
		NULL;

		public static CRMIReleaseExperimentalBehaviorCodes fromCode(String codeString) throws FHIRException {
			if (codeString == null || "".equals(codeString))
				return null;
			if ("error".equals(codeString))
				return ERROR;
			if ("warn".equals(codeString))
				return WARN;
			if ("none".equals(codeString))
				return NONE;
			if (Configuration.isAcceptInvalidEnums())
				return null;
			else
				throw new FHIRException("Unknown CRMIReleaseExperimentalBehaviorCode '" + codeString + "'");
		}

		public String toCode() {
			switch (this) {
				case ERROR:
					return "error";
				case WARN:
					return "warn";
				case NONE:
					return "none";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

		public String getSystem() {
			switch (this) {
				case ERROR:
					return "http://hl7.org/fhir/uv/crmi/CodeSystem/crmi-release-experimental-behavior-codes";
				case WARN:
					return "http://hl7.org/fhir/uv/crmi/CodeSystem/crmi-release-experimental-behavior-codes";
				case NONE:
					return "http://hl7.org/fhir/uv/crmi/CodeSystem/crmi-release-experimental-behavior-codes";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

		public String getDefinition() {
			switch (this) {
				case ERROR:
					return "The repository should throw an error if a specification which is not Experimental references Experimental components.";
				case WARN:
					return "The repository should warn if a specification which is not Experimental references Experimental components.";
				case NONE:
					return "The repository does not need to consider the state of Experimental.";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

		public String getDisplay() {
			switch (this) {
				case ERROR:
					return "Error";
				case WARN:
					return "Warn";
				case NONE:
					return "None";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

	}

	public static class CRMIReleaseExperimentalBehaviorCodesEnumFactory implements EnumFactory<CRMIReleaseExperimentalBehaviorCodes> {
		public CRMIReleaseExperimentalBehaviorCodes fromCode(String codeString) throws IllegalArgumentException {
			if (codeString == null || "".equals(codeString))
				if (codeString == null || "".equals(codeString))
					return null;
			if ("error".equals(codeString))
				return CRMIReleaseExperimentalBehaviorCodes.ERROR;
			if ("warn".equals(codeString))
				return CRMIReleaseExperimentalBehaviorCodes.WARN;
			if ("none".equals(codeString))
				return CRMIReleaseExperimentalBehaviorCodes.NONE;
			throw new IllegalArgumentException("Unknown CRMIReleaseExperimentalBehaviorCodes code '" + codeString + "'");
		}

		public Enumeration<CRMIReleaseExperimentalBehaviorCodes> fromType(Base code) throws FHIRException {
			if (code == null)
				return null;
			if (code.isEmpty())
				return new Enumeration<CRMIReleaseExperimentalBehaviorCodes>(this);
			String codeString = ((PrimitiveType) code).asStringValue();
			if (codeString == null || "".equals(codeString))
				return null;
			if ("error".equals(codeString))
				return new Enumeration<CRMIReleaseExperimentalBehaviorCodes>(this, CRMIReleaseExperimentalBehaviorCodes.ERROR);
				if ("warn".equals(codeString))
					return new Enumeration<CRMIReleaseExperimentalBehaviorCodes>(this, CRMIReleaseExperimentalBehaviorCodes.WARN);
				if ("none".equals(codeString))
					return new Enumeration<CRMIReleaseExperimentalBehaviorCodes>(this, CRMIReleaseExperimentalBehaviorCodes.NONE);
			throw new FHIRException("Unknown CRMIReleaseExperimentalBehaviorCodes code '" + codeString + "'");
		}

		public String toCode(CRMIReleaseExperimentalBehaviorCodes code) {
			if (code == CRMIReleaseExperimentalBehaviorCodes.ERROR)
				return "error";
			if (code == CRMIReleaseExperimentalBehaviorCodes.WARN)
				return "warn";
			if (code == CRMIReleaseExperimentalBehaviorCodes.NONE)
				return "none";
			return "?";
		}

		public String toSystem(CRMIReleaseExperimentalBehaviorCodes code) {
			return code.getSystem();
		}
	}


}
