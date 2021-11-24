package org.opencds.cqf.ruler.plugin.utility;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public interface IdUtilities extends ReflectionUtilities {

    /**
     * Creates the appropriate IIdType for a given ResourceTypeClass
     * 
     * @param <ResourceType> an IBaseResource type
     * @param theResourceTypeClass the type of the Resource to create an Id for
     * @param theId the String representation of the Id to generate
     * @return the id
     */
    public default <ResourceType extends IBaseResource> IIdType createId(Class<ResourceType> theResourceTypeClass, String theId) {
        FhirVersionEnum versionEnum = this.getFhirVersion(theResourceTypeClass);
        return createId(versionEnum, theResourceTypeClass.getSimpleName(), theId);
    }

    /**
     * Creates the appropriate IIdType for a given FhirContext
     * 
     * @param theFhirContext the FhirContext to use for Id generation
     * @param theResourceType the type of the Resource to create an Id for
     * @param theId the String representation of the Id to generate
     * @return the id
     */
    public default IIdType createId(FhirContext theFhirContext, String theResourceType, String theId) {
        return createId(theFhirContext.getVersion().getVersion(), theResourceType, theId);
    }

    /**
     * Creates the appropriate IIdType for a given FhirVersionEnum
     * 
     * @param theFhirVersionEnum the FHIR version to generate an Id for
     * @param theResourceType the type of the Resource to create an Id for
     * @param theId the String representation of the Id to generate
     * @return the id
     */
    public default IIdType createId(FhirVersionEnum theFhirVersionEnum, String theResourceType, String theId) {
        return createId(theFhirVersionEnum, theResourceType + "/" + theId);
    }

    /**
     * Creates the appropriate IIdType for a given FhirContext
     * 
     * @param theFhirContext the FhirContext to use for Id generation
     * @param theId the String representation of the Id to generate
     * @return the id
     */
    public default IIdType createId(FhirContext theFhirContext, String theId) {
        return createId(theFhirContext.getVersion().getVersion(), theId);
    }

    /**
     * Creates the appropriate IIdType for a given FhirVersionEnum
     * 
     * @param theFhirVersionEnum the FHIR version to generate an Id for
     * @param theId the String representation of the Id to generate
     * @return the id
     */
    public default IIdType createId(FhirVersionEnum theFhirVersionEnum, String theId) {
        switch (theFhirVersionEnum) {
        case DSTU2:
            return new ca.uhn.fhir.model.primitive.IdDt(theId);
        case DSTU2_1:
            return new org.hl7.fhir.dstu2016may.model.IdType(theId);
        case DSTU2_HL7ORG:
            return new org.hl7.fhir.dstu2.model.IdType(theId);
        case DSTU3:
            return new org.hl7.fhir.dstu3.model.IdType(theId);
        case R4:
            return new org.hl7.fhir.r4.model.IdType(theId);
        case R5:
            return new org.hl7.fhir.r5.model.IdType(theId);
        default:
            throw new IllegalArgumentException(String.format("createId does not support FHIR version %s",
                    theFhirVersionEnum.getFhirVersionString()));
        }
    }
}
