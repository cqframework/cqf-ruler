package org.opencds.cqf.ruler.behavior;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.ruler.utility.Ids;
import org.opencds.cqf.ruler.utility.TypedBundleProvider;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

/**
 * Simulate FhirDal operations until such time as that is fully baked
 */
public interface DaoRegistryUser {

	public DaoRegistry getDaoRegistry();

	/**
	 * @param <T> the Resource type to read
	 * @param theResourceClass the Resource type to read
	 * @param theIdPart the logical ID part of this ID. For example, "123"
	 * @throws ResourceNotFoundException if the Id is not known
	 */
	default <T extends IBaseResource> T read(Class<T> theResourceClass, String theIdPart) {
		checkNotNull(theResourceClass);
		checkNotNull(theIdPart);

		return read(theResourceClass, theIdPart, null);
	}

	/**
	 * @param <T> the Resource type to read
	 * @param theResourceClass the Resource type to read
	 * @param theIdPart the logical ID part of this ID. For example, "123"
	 * @param requestDetails multi-tenancy information
	 * @throws ResourceNotFoundException if the Id is not known
	 */
	default <T extends IBaseResource> T read(Class<T> theResourceClass, String theIdPart, RequestDetails requestDetails) {
		checkNotNull(theResourceClass);
		checkNotNull(theIdPart);
		checkNotNull(requestDetails);

		return getDaoRegistry().getResourceDao(theResourceClass).read(Ids.newId(theResourceClass, theIdPart), requestDetails);
	}

	/**
	 * Reads the the given Id from the local server
	 * @param <T> the Resource type to read
	 * @param theId the id to read
	 * @return the FHIR Resource
	 * @throws ResourceNotFoundException if the Id is not known
	 */
	default <T extends IBaseResource> T read(IIdType theId) {
		checkNotNull(theId);

		return read(theId, null);
	}

	/**
	 * Reads the the given Id from the local server
	 * @param <T> the Resource type to read
	 * @param theId the id to read
	 * @param requestDetails multi-tenancy information
	 * @return the FHIR Resource
	 * @throws ResourceNotFoundException if the Id is not known
	 */
	@SuppressWarnings("unchecked")
	default <T extends IBaseResource> T read(IIdType theId, RequestDetails requestDetails) {
		checkNotNull(theId);

		return (T) getDaoRegistry().getResourceDao(theId.getResourceType()).read(theId, requestDetails);
	}

	default <T extends IBaseResource> DaoMethodOutcome create(T theResource) {
		checkNotNull(theResource);

		return create(theResource, null);
	}

	@SuppressWarnings("unchecked")
	default <T extends IBaseResource> DaoMethodOutcome create(T theResource, RequestDetails requestDetails) {
		checkNotNull(theResource);

		return ((IFhirResourceDao<T>) getDaoRegistry().getResourceDao(theResource.fhirType())).create(theResource,
				requestDetails);
	}

	default <T extends IBaseResource> DaoMethodOutcome update(T theResource) {
		checkNotNull(theResource);

		return update(theResource, null);
	}

	@SuppressWarnings("unchecked")
	default <T extends IBaseResource> DaoMethodOutcome update(T theResource, RequestDetails requestDetails) {
		checkNotNull(theResource);

		return ((IFhirResourceDao<T>) getDaoRegistry().getResourceDao(theResource.fhirType())).update(theResource,
				requestDetails);
	}

	default <T extends IBaseResource> DaoMethodOutcome delete(T theResource) {
		checkNotNull(theResource);

		return delete(theResource, null);
	}

	@SuppressWarnings("unchecked")
	default <T extends IBaseResource> DaoMethodOutcome delete(T theResource, RequestDetails requestDetails) {
		checkNotNull(theResource);

		return ((IFhirResourceDao<T>) getDaoRegistry().getResourceDao(theResource.fhirType()))
				.delete(theResource.getIdElement(), requestDetails);
	}

	default DaoMethodOutcome delete(IIdType theIdType) {
		checkNotNull(theIdType);

		return delete(theIdType, null);
	}

	default DaoMethodOutcome delete(IIdType theIdType, RequestDetails requestDetails) {
		checkNotNull(theIdType);

		return getDaoRegistry().getResourceDao(theIdType.getResourceType()).delete(theIdType, requestDetails);
	}

	default <T extends IBaseBundle> T transaction(T theTransaction) {
		checkNotNull(theTransaction);

		return transaction(theTransaction, null);
	}

	@SuppressWarnings("unchecked")
	default <T extends IBaseBundle> T transaction(T theTransaction, RequestDetails theRequestDetails) {
		checkNotNull(theTransaction);

		return (T) getDaoRegistry().getSystemDao().transaction(theRequestDetails, theTransaction);
	}

	default <T extends IBaseResource> TypedBundleProvider<T> search(Class<T> theResourceClass,
			SearchParameterMap theSearchMap) {
		checkNotNull(theResourceClass);
		checkNotNull(theSearchMap);

		return search(theResourceClass, theSearchMap, null);
	}

	default <T extends IBaseResource> TypedBundleProvider<T> search(Class<T> theResourceClass, SearchParameterMap theSearchMap,
			RequestDetails theRequestDetails) {
		checkNotNull(theResourceClass);
		checkNotNull(theSearchMap);

		return TypedBundleProvider.fromBundleProvider(getDaoRegistry().getResourceDao(theResourceClass).search(theSearchMap, theRequestDetails));
	}

	default IBundleProvider search(String theResourceName, SearchParameterMap theSearchMap) {
		checkNotNull(theResourceName);
		checkNotNull(theSearchMap);

		return search(theResourceName, theSearchMap, null);
	}

	default IBundleProvider search(String theResourceName, SearchParameterMap theSearchMap,
			RequestDetails theRequestDetails) {
		checkNotNull(theResourceName);
		checkNotNull(theSearchMap);

		return getDaoRegistry().getResourceDao(theResourceName).search(theSearchMap);
	}
}
