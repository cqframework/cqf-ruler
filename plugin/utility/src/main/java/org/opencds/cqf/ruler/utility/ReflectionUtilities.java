package org.opencds.cqf.ruler.utility;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition.IAccessor;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.RuntimeResourceDefinition;

/**
 * This interface provides utility methods for doing reflection on FHIR
 * resources. It's specifically focused on knowledge artifact resources since
 * there's not a common interface for those across different Resources (and FHIR
 * versions)
 */
public interface ReflectionUtilities {
	/**
	 * Returns a FhirVersionEnum for a given BaseType
	 * 
	 * @param <BaseType>       an IBase type
	 * @param theBaseTypeClass the class of the resource to get the version for
	 * @return the FhirVersionEnum corresponding to the theBaseTypeClass
	 */
	default <BaseType extends IBase> FhirVersionEnum getFhirVersion(
			final Class<? extends BaseType> theBaseTypeClass) {
		String packageName = theBaseTypeClass.getPackage().getName();
		if (packageName.contains("r5")) {
			return FhirVersionEnum.R5;
		} else if (packageName.contains("r4")) {
			return FhirVersionEnum.R4;
		} else if (packageName.contains("dstu3")) {
			return FhirVersionEnum.DSTU3;
		} else if (packageName.contains("dstu2016may")) {
			return FhirVersionEnum.DSTU2_1;
		} else if (packageName.contains("org.hl7.fhir.dstu2")) {
			return FhirVersionEnum.DSTU2_HL7ORG;
		} else if (packageName.contains("ca.uhn.fhir.model.dstu2")) {
			return FhirVersionEnum.DSTU2;
		} else {
			throw new IllegalArgumentException(String.format(
					"Unable to determine FHIR version for IBaseResource type: %s", theBaseTypeClass.getName()));
		}
	}

	/**
	 * Gets the IAccessor for the given BaseType and child
	 * 
	 * @param <BaseType>       an IBase type
	 * @param theBaseTypeClass the class of a the IBase type
	 * @param theChildName         the name of the child property of the
	 *                             BaseType to generate an accessor for
	 * @return an IAccessor for the given child and the BaseType
	 */
	default <BaseType extends IBase> IAccessor getAccessor(
			final Class<? extends BaseType> theBaseTypeClass, String theChildName) {
		FhirContext fhirContext = FhirContext.forCached(this.getFhirVersion(theBaseTypeClass));
		if (theBaseTypeClass.isInstance(IBaseResource.class)) {
			@SuppressWarnings("unchecked")
			RuntimeResourceDefinition resourceDefinition = fhirContext
					.getResourceDefinition((Class<? extends IBaseResource>) theBaseTypeClass);
			return resourceDefinition.getChildByName(theChildName).getAccessor();
		} else {
			BaseRuntimeElementDefinition<?> elementDefinition = fhirContext.getElementDefinition(theBaseTypeClass);
			return elementDefinition.getChildByName(theChildName).getAccessor();
		}
	}

	/**
	 * Generates a function to access a primitive property of the given
	 * BaseType.
	 * 
	 * @param <BaseType>       an IBase type
	 * @param <ReturnType>         a return type for the Functions
	 * @param theBaseTypeClass the class of a the IBase type
	 * @param theChildName         to create a function for
	 * @return a function for accessing the "theChildName" property of the
	 *         BaseType
	 */
	@SuppressWarnings("unchecked")
	default <BaseType extends IBase, ReturnType> Function<BaseType, ReturnType> getPrimitiveFunction(
			final Class<? extends BaseType> theBaseTypeClass, String theChildName) {
		IAccessor accessor = this.getAccessor(theBaseTypeClass, theChildName);
		return r -> {
			Optional<IBase> value = accessor.getFirstValueOrNull(r);
			if (!value.isPresent()) {
				return null;
			} else {
				return ((IPrimitiveType<ReturnType>) value.get()).getValue();
			}
		};
	}

	/**
	 * Generates a function to access a primitive property of the given
	 * BaseType.
	 * 
	 * @param <BaseType>       an IBase type
	 * @param <ReturnType>         a return type for the Functions
	 * @param theBaseTypeClass the class of a the IBase type
	 * @param theChildName         to create a function for
	 * @return a function for accessing the "theChildName" property of the
	 *         BaseType
	 */
	@SuppressWarnings("unchecked")
	default <BaseType extends IBase, ReturnType extends List<? extends IBase>> Function<BaseType, ReturnType> getFunction(
			final Class<? extends BaseType> theBaseTypeClass, String theChildName) {
		IAccessor accessor = this.getAccessor(theBaseTypeClass, theChildName);
		return r -> {
			return (ReturnType) accessor.getValues(r);
		};
	}

	/**
	 * Generates a function to access the "version" property of the given
	 * BaseType.
	 * 
	 * @param <BaseType>       an IBase type
	 * @param theBaseTypeClass the class of a the IBase type
	 * @return a function for accessing the "version" property of the BaseType
	 */
	default <BaseType extends IBase> Function<BaseType, String> getVersionFunction(
			final Class<? extends BaseType> theBaseTypeClass) {
		return this.getPrimitiveFunction(theBaseTypeClass, "version");
	}

	/**
	 * Generates a function to access the "url" property of the given BaseType.
	 * 
	 * @param <BaseType>       an IBase type
	 * @param theBaseTypeClass the class of a the IBase type
	 * @return a function for accessing the "url" property of the BaseType
	 */
	default <BaseType extends IBase> Function<BaseType, String> getUrlFunction(
			final Class<? extends BaseType> theBaseTypeClass) {
		return this.getPrimitiveFunction(theBaseTypeClass, "url");
	}

	/**
	 * Generates a function to access the "name" property of the given BaseType.
	 * 
	 * @param <BaseType>       an IBase type
	 * @param theBaseTypeClass the class of a the IBase type
	 * @return a function for accessing the "name" property of the BaseType
	 */
	default <BaseType extends IBase> Function<BaseType, String> getNameFunction(
			final Class<? extends BaseType> theBaseTypeClass) {
		return this.getPrimitiveFunction(theBaseTypeClass, "name");
	}
}
