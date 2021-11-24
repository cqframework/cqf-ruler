package org.opencds.cqf.ruler.plugin.utility;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;

/**
 * This interface provides utility functions for resolving FHIR resources based
 * on id, name and version, or url. It supplies Tenant-aware overloads for the
 * resolution functions as well.
 * 
 * TODO: Eventually we need FhirDal versions of these functions.
 */
public interface ResolutionUtilities extends IdUtilities, VersionUtilities {
	/**
	 * Returns the Resource with a matching Id
	 * 
	 * @param <ResourceType>    an IBaseResource type
	 * @param theDaoRegistry    the DaoRegistry to use for resolution
	 * @param theId             the Id of the Resource to resolve
	 * @param theRequestDetails the RequestDetails to use for resolution. Use this
	 *                          parameter to select a tenant.
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveById(DaoRegistry theDaoRegistry,
			IIdType theId) {
		return this.resolveById(theDaoRegistry, theId, null);
	}

	/**
	 * Returns the Resource with a matching Id
	 * 
	 * @param <ResourceType> an IBaseResource type
	 * @param theDaoRegistry the DaoRegistry to use for resolution
	 * @param theId          the Id of the Resource to resolve
	 * @return the Resource matching the criteria
	 */
	@SuppressWarnings("unchecked")
	public default <ResourceType extends IBaseResource> ResourceType resolveById(DaoRegistry theDaoRegistry,
			IIdType theId, RequestDetails requestDetails) {
		if (theId.getResourceType() == null) {
			throw new IllegalArgumentException("theId does not have a resourceType set.");
		}
		return (ResourceType) this.resolveById(theDaoRegistry.getResourceDao(theId.getResourceType()), theId,
				requestDetails);
	}

	/**
	 * Returns the Resource with a matching Id
	 * 
	 * @param <ResourceType>  an IBaseResource type
	 * @param theDaoRegistry  the DaoRegistry to use for resolution
	 * @param theResourceType the class of the Resource
	 * @param theId           the Id of the Resource to resolve
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveById(DaoRegistry theDaoRegistry,
			Class<ResourceType> theResourceType, IIdType theId) {
		return this.resolveById(theDaoRegistry.getResourceDao(theResourceType), theId, null);
	}

	/**
	 * Returns the Resource with a matching Id
	 * 
	 * @param <ResourceType>  an IBaseResource type
	 * @param theDaoRegistry  the DaoRegistry to use for resolution
	 * @param theResourceType the class of the Resource
	 * @param theId           the Id of the Resource to resolve
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveById(DaoRegistry theDaoRegistry,
			Class<ResourceType> theResourceType, String theId) {
		return this.resolveById(theDaoRegistry.getResourceDao(theResourceType), theId, null);
	}

	/**
	 * Returns the Resource with a matching Id. Tenant aware.
	 * 
	 * @param <ResourceType>    an IBaseResource type
	 * @param theDaoRegistry    the DaoRegistry to use for resolution
	 * @param theResourceType   the class of the Resource
	 * @param theId             the Id of the Resource to resolve
	 * @param theRequestDetails the RequestDetails to use for resolution. Use this
	 *                          parameter to select a tenant.
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveById(DaoRegistry theDaoRegistry,
			Class<ResourceType> theResourceType, IIdType theId, RequestDetails theRequestDetails) {
		return this.resolveById(theDaoRegistry.getResourceDao(theResourceType), theId, theRequestDetails);
	}

	/**
	 * Returns the Resource with a matching Id. Tenant aware.
	 * 
	 * @param <ResourceType>    an IBaseResource type
	 * @param theDaoRegistry    the DaoRegistry to use for resolution
	 * @param theResourceType   the class of the Resource
	 * @param theId             the Id of the Resource to resolve
	 * @param theRequestDetails the RequestDetails to use for resolution. Use this
	 *                          parameter to select a tenant.
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveById(DaoRegistry theDaoRegistry,
			Class<ResourceType> theResourceType, String theId, RequestDetails theRequestDetails) {
		return this.resolveById(theDaoRegistry.getResourceDao(theResourceType), theId, theRequestDetails);
	}

	/**
	 * Returns the Resource with a matching Id
	 * 
	 * @param <ResourceType> an IBaseResource type
	 * @param theResourceDao the IFhirResourceDao to use for resolution
	 * @param theId          the Id of the Resource to resolve.
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveById(
			IFhirResourceDao<ResourceType> theResourceDao, IIdType theId) {
		return this.resolveById(theResourceDao, theId, null);
	}

	/**
	 * Returns the Resource with a matching Id
	 * 
	 * @param <ResourceType> an IBaseResource type
	 * @param theResourceDao the IFhirResourceDao to use for resolution
	 * @param theId          the Id of the Resource to resolve.
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveById(
			IFhirResourceDao<ResourceType> theResourceDao, String theId) {
		return this.resolveById(theResourceDao, theId, null);
	}

	/**
	 * Returns the Resource with a matching Id, tenant aware
	 * 
	 * @param <ResourceType>    an IBaseResource type
	 * @param theResourceDao    the IFhirResourceDao to use for resolution
	 * @param theId             the Id of the Resource to resolve.
	 * @param theRequestDetails the RequestDetails to use for resolution. Use this
	 *                          parameter to select a tenant.
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveById(
			IFhirResourceDao<ResourceType> theResourceDao, String theId, RequestDetails theRequestDetails) {
		IIdType id = this.createId(theResourceDao.getContext(), theId);
		return this.resolveById(theResourceDao, id, theRequestDetails);
	}

	/**
	 * Returns the Resource with a matching Id, tenant aware
	 * 
	 * @param <ResourceType>    an IBaseResource type
	 * @param theResourceDao    the IFhirResourceDao to use for resolution
	 * @param theId             the Id of the Resource to resolve.
	 * @param theRequestDetails the RequestDetails to use for resolution. Use this
	 *                          parameter to select a tenant.
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveById(
			IFhirResourceDao<ResourceType> theResourceDao, IIdType theId, RequestDetails theRequestDetails) {
		return theResourceDao.read(theId, theRequestDetails);
	}

	/**
	 * Returns a Resource with a matching name. The highest matching version is
	 * returned if more than one resource is found.
	 * 
	 * @param <ResourceType>  an IBaseResource type
	 * @param theDaoRegistry  the DaoRegistry to use for resolution
	 * @param theResourceType the class of the Resource
	 * @param theName         the name of the Resource
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByName(DaoRegistry theDaoRegistry,
			Class<ResourceType> theResourceType, String theName) {
		return resolveByNameAndVersion(theDaoRegistry.getResourceDao(theResourceType), null, theName, null);
	}

	/**
	 * Returns a Resource with a matching name. The highest matching version is
	 * returned if more than one resource is found. Tenant aware.
	 * 
	 * @param <ResourceType>    an IBaseResource type
	 * @param theDaoRegistry    the DaoRegistry to use for resolution
	 * @param theResourceType   the class of the Resource
	 * @param theRequestDetails the RequestDetails to use for resolution. Use this
	 *                          parameter to select a tenant.
	 * @param theName           the name of the Resource
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByName(DaoRegistry theDaoRegistry,
			Class<ResourceType> theResourceType, String theName, RequestDetails theRequestDetails) {
		return resolveByNameAndVersion(theDaoRegistry.getResourceDao(theResourceType), null, theName,
				theRequestDetails);
	}

	/**
	 * Returns a Resource with a matching name. The highest matching version is
	 * returned if more than one resource is found.
	 * 
	 * @param <ResourceType> an IBaseResource type
	 * @param theResourceDao the IFhirResourceDao to use for resolution
	 * @param theName        the name of the Resource
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByName(
			IFhirResourceDao<ResourceType> theResourceDao, String theName) {
		return resolveByNameAndVersion(theResourceDao, null, theName, null);
	}

	/**
	 * Returns a Resource with a matching name. The highest matching version is
	 * returned if more than one resource is found. Tenant aware.
	 * 
	 * @param <ResourceType>    an IBaseResource type
	 * @param theResourceDao    the IFhirResourceDao to use for resolution
	 * @param theRequestDetails the RequestDetails to use for resolution. Use this
	 *                          parameter to select a tenant.
	 * @param theName           the name of the Resource
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByName(
			IFhirResourceDao<ResourceType> theResourceDao, String theName, RequestDetails theRequestDetails) {
		return resolveByNameAndVersion(theResourceDao, theName, null, theRequestDetails);
	}

	/**
	 * Returns a Resource with a matching name and version. The highest matching
	 * version is returned no direct match is found.
	 * 
	 * @param <ResourceType>  an IBaseResource type
	 * @param theDaoRegistry  the DaoRegistry to use for resolution
	 * @param theResourceType the class of the Resource
	 * @param theName         the name of the Resource
	 * @param theVersion      the business version of the Resource
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByNameAndVersion(DaoRegistry theDaoRegistry,
			Class<ResourceType> theResourceType, String theName, String theVersion) {
		return this.resolveByNameAndVersion(theDaoRegistry.getResourceDao(theResourceType), theName, theVersion, null);
	}

	/**
	 * Returns a Resource with a matching name and version. The highest matching
	 * version is returned no direct match is found. Tenant aware.
	 * 
	 * @param <ResourceType>    an IBaseResource type
	 * @param theDaoRegistry    the DaoRegistry to use for resolution
	 * @param theResourceType   the class of the Resource
	 * @param theName           the name of the Resource
	 * @param theVersion        the business version of the Resource
	 * @param theRequestDetails the RequestDetails to use for resolution. Use this
	 *                          parameter to select a tenant.
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByNameAndVersion(DaoRegistry theDaoRegistry,
			Class<ResourceType> theResourceType, String theName, String theVersion, RequestDetails theRequestDetails) {
		return this.resolveByNameAndVersion(theDaoRegistry.getResourceDao(theResourceType), theName, theVersion,
				theRequestDetails);
	}

	/**
	 * Returns a Resource with a matching name and version. The highest matching
	 * version is returned no direct match is found.
	 * 
	 * @param <ResourceType> an IBaseResource type
	 * @param theResourceDao the IFhirResourceDao to use for resolution
	 * @param theName        the name of the Resource
	 * @param theVersion     the business version of the Resource
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByNameAndVersion(
			IFhirResourceDao<ResourceType> theResourceDao, String theName, String theVersion) {
		return this.resolveByNameAndVersion(theResourceDao, theName, theVersion, null);
	}

	/**
	 * Returns a Resource with a matching name and version. The highest matching
	 * version is returned no direct match is found. Tenant aware.
	 * 
	 * @param <ResourceType>    an IBaseResource type
	 * @param theResourceDao    the IFhirResourceDao to use for resolution
	 * @param theName           the name of the Resource
	 * @param theVersion        the business version of the Resource
	 * @param theRequestDetails the RequestDetails to use for resolution. Use this
	 *                          parameter to select a tenant.
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByNameAndVersion(
			IFhirResourceDao<ResourceType> theResourceDao, String theName, String theVersion,
			RequestDetails theRequestDetails) {
		@SuppressWarnings("unchecked")
		List<ResourceType> resources = (List<ResourceType>) theResourceDao
				.search(SearchParameterMap.newSynchronous().add("name", new TokenParam(theName))).getAllResources();

		return this.selectFromList(resources, theVersion,
				this.getVersionFunction(theResourceDao.getContext(), theResourceDao.getResourceType()));
	}

	/**
	 * Returns a Resource with a matching canonical url. If the canonical url
	 * contains a version the matching version is returned. If the canonical url
	 * does not contain a version, or if no matching version is found, the highest
	 * version is returned.
	 * 
	 * @param <ResourceType>  an IBaseResource type
	 * @param theDaoRegistry  the DaoRegistry to use for resolution
	 * @param theResourceType the class of the Resource
	 * @param theUrl          the url of the Resource to resolve
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByCanonicalUrl(DaoRegistry theDaoRegistry,
			Class<ResourceType> theResourceType, String theUrl) {
		return this.resolveByCanonicalUrl(theDaoRegistry.getResourceDao(theResourceType), theUrl, null);
	}

	/**
	 * Returns a Resource with a matching canonical url. If the canonical url
	 * contains a version the matching version is returned. If the canonical url
	 * does not contain a version, or if no matching version is found, the highest
	 * version is returned. Tenant aware.
	 * 
	 * @param <ResourceType>    an IBaseResource type
	 * @param theDaoRegistry    the DaoRegistry to use for resolution
	 * @param theResourceType   the class of the Resource
	 * @param theUrl            the url of the Resource to resolve
	 * @param theRequestDetails the RequestDetails to use for resolution. Use this
	 *                          parameter to select a tenant.
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByCanonicalUrl(DaoRegistry theDaoRegistry,
			Class<ResourceType> theResourceType, String theUrl, RequestDetails theRequestDetails) {
		return this.resolveByCanonicalUrl(theDaoRegistry.getResourceDao(theResourceType), theUrl, theRequestDetails);
	}

	/**
	 * Returns a Resource with a matching canonical url. If the canonical url
	 * contains a version the matching version is returned. If the canonical url
	 * does not contain a version, or if no matching version is found, the highest
	 * version is returned.
	 * 
	 * @param <ResourceType> an IBaseResource type
	 * @param theResourceDao the IFhirResourceDao to use for resolution
	 * @param theUrl         the url of the Resource to resolve
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByCanonicalUrl(
			IFhirResourceDao<ResourceType> theResourceDao, String theUrl) {
		return this.resolveByCanonicalUrl(theResourceDao, theUrl, null);
	}

	/**
	 * Returns a Resource with a matching canonical url. If the canonical url
	 * contains a version the matching version is returned. If the canonical url
	 * does not contain a version, or if no matching version is found, the highest
	 * version is returned. Tenant aware.
	 * 
	 * @param <ResourceType>    an IBaseResource type
	 * @param theResourceDao    the IFhirResourceDao to use for resolution
	 * @param theUrl            the url of the Resource to resolve
	 * @param theRequestDetails the RequestDetails to use for resolution. Use this
	 *                          parameter to select a tenant.
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByCanonicalUrl(
			IFhirResourceDao<ResourceType> theResourceDao, String theUrl, RequestDetails theRequestDetails) {
		String[] urlParts = theUrl.split("|");
		String url = urlParts[0];
		String version = null;
		if (urlParts.length > 1) {
			version = urlParts[1];
		}

		@SuppressWarnings("unchecked")
		List<ResourceType> resources = (List<ResourceType>) theResourceDao
				.search(SearchParameterMap.newSynchronous().add("url", new UriParam(url))).getAllResources();

		return this.selectFromList(resources, version,
				this.getVersionFunction(theResourceDao.getContext(), theResourceDao.getResourceType()));
	}

	/**
	 * Returns a Resource with a matching canonical url. If the canonical url
	 * contains a version the matching version is returned. If the canonical url
	 * does not contain a version, or if no matching version is found, the highest
	 * version is returned.
	 * 
	 * @param <ResourceType>  an IBaseResource type
	 * @param theDaoRegistry  the DaoRegistry to use for resolution
	 * @param theResourceType the class of the Resource
	 * @param theUrl          the url of the Resource to resolve
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByCanonicalUrl(DaoRegistry theDaoRegistry,
			Class<ResourceType> theResourceType, IPrimitiveType<String> theUrl) {
		return this.resolveByCanonicalUrl(theDaoRegistry.getResourceDao(theResourceType), theUrl.getValue(), null);
	}

	/**
	 * Returns a Resource with a matching canonical url. If the canonical url
	 * contains a version the matching version is returned. If the canonical url
	 * does not contain a version, or if no matching version is found, the highest
	 * version is returned. Tenant aware.
	 * 
	 * @param <ResourceType>    an IBaseResource type
	 * @param theDaoRegistry    the DaoRegistry to use for resolution
	 * @param theResourceType   the class of the Resource
	 * @param theUrl            the url of the Resource to resolve
	 * @param theRequestDetails the RequestDetails to use for resolution. Use this
	 *                          parameter to select a tenant.
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByCanonicalUrl(DaoRegistry theDaoRegistry,
			Class<ResourceType> theResourceType, IPrimitiveType<String> theUrl, RequestDetails theRequestDetails) {
		return this.resolveByCanonicalUrl(theDaoRegistry.getResourceDao(theResourceType), theUrl.getValue(),
				theRequestDetails);
	}

	/**
	 * Returns a Resource with a matching canonical url. If the canonical url
	 * contains a version the matching version is returned. If the canonical url
	 * does not contain a version, or if no matching version is found, the highest
	 * version is returned.
	 * 
	 * @param <ResourceType> an IBaseResource type
	 * @param theResourceDao the IFhirResourceDao to use for resolution
	 * @param theUrl         the url of the Resource to resolve
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByCanonicalUrl(
			IFhirResourceDao<ResourceType> theResourceDao, IPrimitiveType<String> theUrl) {
		return this.resolveByCanonicalUrl(theResourceDao, theUrl.getValue(), null);
	}

	/**
	 * Returns a Resource with a matching canonical url. If the canonical url
	 * contains a version the matching version is returned. If the canonical url
	 * does not contain a version, or if no matching version is found, the highest
	 * version is returned. Tenant aware.
	 * 
	 * @param <ResourceType>    an IBaseResource type
	 * @param theResourceDao    the IFhirResourceDao to use for resolution
	 * @param theUrl            the url of the Resource to resolve
	 * @param theRequestDetails the RequestDetails to use for resolution. Use this
	 *                          parameter to select a tenant.
	 * @return the Resource matching the criteria
	 */
	public default <ResourceType extends IBaseResource> ResourceType resolveByCanonicalUrl(
			IFhirResourceDao<ResourceType> theResourceDao, IPrimitiveType<String> theUrl,
			RequestDetails theRequestDetails) {
		return this.resolveByCanonicalUrl(theResourceDao, theUrl.getValue(), theRequestDetails);
	}
}
