package org.opencds.cqf.ruler.common.utility;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class Ids {

	private Ids() {}

	/**
	 * Creates the appropriate IIdType for a given ResourceTypeClass
	 * 
	 * @param <ResourceType>       an IBase type
	 * @param <IdType>             an IIdType type
	 * @param theResourceTypeClass the type of the Resource to create an Id for
	 * @param theId                the String representation of the Id to generate
	 * @return the id
	 */
	public static <ResourceType extends IBaseResource, IdType extends IIdType> IdType newId(
			Class<? extends ResourceType> theResourceTypeClass, String theId) {
		checkNotNull(theResourceTypeClass);
		checkNotNull(theId);

		FhirVersionEnum versionEnum = FhirVersions.forClass(theResourceTypeClass);
		return newId(versionEnum, theResourceTypeClass.getSimpleName(), theId);
	}

	/**
	 * Creates the appropriate IIdType for a given BaseTypeClass
	 * 
	 * @param <BaseType>       an IBase type
	 * @param <IdType>         an IIdType type
	 * @param theBaseTypeClass the BaseTypeClass to use for for determining the FHIR
	 *                         Version
	 * @param theResourceName  the type of the Resource to create an Id for
	 * @param theId            the String representation of the Id to generate
	 * @return the id
	 */
	public static <BaseType extends IBase, IdType extends IIdType> IdType newId(
			Class<? extends BaseType> theBaseTypeClass, String theResourceName, String theId) {
		checkNotNull(theBaseTypeClass);
		checkNotNull(theResourceName);
		checkNotNull(theId);

		FhirVersionEnum versionEnum = FhirVersions.forClass(theBaseTypeClass);
		return newId(versionEnum, theResourceName, theId);
	}

	/**
	 * Creates the appropriate IIdType for a given FhirContext
	 * 
	 * @param <IdType>        an IIdType type
	 * @param theFhirContext  the FhirContext to use for Id generation
	 * @param theResourceType the type of the Resource to create an Id for
	 * @param theId           the String representation of the Id to generate
	 * @return the id
	 */
	public static <IdType extends IIdType> IdType newId(FhirContext theFhirContext, String theResourceType,
			String theId) {
		checkNotNull(theFhirContext);
		checkNotNull(theResourceType);
		checkNotNull(theId);

		return newId(theFhirContext.getVersion().getVersion(), theResourceType, theId);
	}

	/**
	 * Creates the appropriate IIdType for a given FhirVersionEnum
	 * 
	 * @param <IdType>           an IIdType type
	 * @param theFhirVersionEnum the FHIR version to generate an Id for
	 * @param theResourceType    the type of the Resource to create an Id for
	 * @param theId              the String representation of the Id to generate
	 * @return the id
	 */
	public static <IdType extends IIdType> IdType newId(FhirVersionEnum theFhirVersionEnum, String theResourceType,
			String theId) {
		checkNotNull(theFhirVersionEnum);
		checkNotNull(theResourceType);
		checkNotNull(theId);

		return newId(theFhirVersionEnum, theResourceType + "/" + theId);
	}

	/**
	 * Creates the appropriate IIdType for a given FhirContext
	 * 
	 * @param <IdType>       an IIdType type
	 * @param theFhirContext the FhirContext to use for Id generation
	 * @param theId          the String representation of the Id to generate
	 * @return the id
	 */
	public static <IdType extends IIdType> IdType newId(FhirContext theFhirContext, String theId) {
		checkNotNull(theFhirContext);
		checkNotNull(theId);

		return newId(theFhirContext.getVersion().getVersion(), theId);
	}

	/**
	 * Creates the appropriate IIdType for a given FhirVersionEnum
	 * 
	 * @param <IdType>           an IIdType type
	 * @param theFhirVersionEnum the FHIR version to generate an Id for
	 * @param theId              the String representation of the Id to generate
	 * @return the id
	 */
	@SuppressWarnings("unchecked")
	public static <IdType extends IIdType> IdType newId(FhirVersionEnum theFhirVersionEnum, String theId) {
		checkNotNull(theFhirVersionEnum);
		checkNotNull(theId);

		switch (theFhirVersionEnum) {
			case DSTU2:
				return (IdType) new ca.uhn.fhir.model.primitive.IdDt(theId);
			case DSTU2_1:
				return (IdType) new org.hl7.fhir.dstu2016may.model.IdType(theId);
			case DSTU2_HL7ORG:
				return (IdType) new org.hl7.fhir.dstu2.model.IdType(theId);
			case DSTU3:
				return (IdType) new org.hl7.fhir.dstu3.model.IdType(theId);
			case R4:
				return (IdType) new org.hl7.fhir.r4.model.IdType(theId);
			case R5:
				return (IdType) new org.hl7.fhir.r5.model.IdType(theId);
			default:
				throw new IllegalArgumentException(String.format("createId does not support FHIR version %s",
						theFhirVersionEnum.getFhirVersionString()));
		}
	}
}
