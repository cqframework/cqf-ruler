package org.opencds.cqf.ruler.plugin.utility;

import java.util.Optional;
import java.util.function.Function;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.context.BaseRuntimeChildDefinition.IAccessor;

/**
 * This interface provides utility methods for doing reflection on FHIR
 * resources. It's specifically focused on knowledge artifact resources since
 * there's not a common interface for those across different Resources (and FHIR
 * versions)
 */
public interface ReflectionUtilities {
    /**
     * Returns a FhirVersionEnum for a given ResourceType
     * 
     * @param <ResourceType>       an IBaseResource type
     * @param theResourceTypeClass the class of the resource to get the version for
     * @return the FhirVersionEnum corresponding to the theResourceTypeClass
     */
    public default <ResourceType extends IBaseResource> FhirVersionEnum getFhirVersion(final 
            Class<? extends ResourceType> theResourceTypeClass) {
        String packageName = theResourceTypeClass.getPackage().getName();
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
                    "Unable to determine FHIR version for IBaseResource type: %s", theResourceTypeClass.getName()));
        }
    }

    /**
     * Gets the IAccessor for the given ResourceType and child
     * 
     * @param <ResourceType>       an IBaseResource type
     * @param theResourceTypeClass the class of a the IBaseResource type
     * @param theChildName         the name of the child property of the
     *                             ResourceType to generate an accessor for
     * @return an IAccessor for the given child and the ResourceType
     */
    public default <ResourceType extends IBaseResource> IAccessor getAccessor(final Class<? extends ResourceType> theResourceTypeClass, String theChildName) {
        FhirContext fhirContext = FhirContext.forCached(this.getFhirVersion(theResourceTypeClass));
        RuntimeResourceDefinition resourceDefinition = fhirContext.getResourceDefinition(theResourceTypeClass);
        return resourceDefinition.getChildByName(theChildName).getAccessor();
    }

    /**
     * Generates a function to access a primitive property of the given
     * ResourceType.
     * 
     * @param <ResourceType>       an IBaseResource type
     * @param <ReturnType>         a return type for the Functions
     * @param theResourceTypeClass the class of a the IBaseResource type
     * @param theChildName         to create a function for
     * @return a function for accessing the "theChildName" property of the
     *         ResourceType
     */
    @SuppressWarnings("unchecked")
    public default <ResourceType extends IBaseResource, ReturnType> Function<ResourceType, ReturnType> getFunction(final Class<? extends ResourceType> theResourceTypeClass, String theChildName) {
        IAccessor accessor = this.getAccessor(theResourceTypeClass, theChildName);
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
     * Generates a function to access the "version" property of the given
     * ResourceType.
     * 
     * @param <ResourceType>       an IBaseResource type
     * @param theResourceTypeClass the class of a the IBaseResource type
     * @return a function for accessing the "version" property of the ResourceType
     */
    public default <ResourceType extends IBaseResource> Function<ResourceType, String> getVersionFunction(final Class<? extends ResourceType> theResourceTypeClass) {
        return this.getFunction(theResourceTypeClass, "version");
    }

    /**
     * Generates a function to access the "url" property of the given ResourceType.
     * 
     * @param <ResourceType>       an IBaseResource type
     * @param theResourceTypeClass the class of a the IBaseResource type
     * @return a function for accessing the "url" property of the ResourceType
     */
    public default <ResourceType extends IBaseResource> Function<ResourceType, String> getUrlFunction(final Class<? extends ResourceType> theResourceTypeClass) {
        return this.getFunction(theResourceTypeClass, "url");
    }

    /**
     * Generates a function to access the "name" property of the given ResourceType.
     * 
     * @param <ResourceType>       an IBaseResource type
     * @param theResourceTypeClass the class of a the IBaseResource type
     * @return a function for accessing the "name" property of the ResourceType
     */
    public default <ResourceType extends IBaseResource> Function<ResourceType, String> getNameFunction(final Class<? extends ResourceType> theResourceTypeClass) {
        return this.getFunction(theResourceTypeClass, "name");
    }
}
