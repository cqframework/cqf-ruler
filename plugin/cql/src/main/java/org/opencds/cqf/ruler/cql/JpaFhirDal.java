package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.UriParam;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class JpaFhirDal implements FhirDal {

	protected final DaoRegistry daoRegistry;
	protected final RequestDetails requestDetails;

	public JpaFhirDal(DaoRegistry daoRegistry) {
		this(daoRegistry, null);
	}

	public JpaFhirDal(DaoRegistry daoRegistry, RequestDetails requestDetails) {
		this.daoRegistry = daoRegistry;
		this.requestDetails = requestDetails;
	}

	@Override
	public void create(IBaseResource theResource) {
		this.daoRegistry.getResourceDao(theResource.fhirType()).create(theResource, requestDetails);
	}

	@Override
	public IBaseResource read(IIdType theId) {
		return this.daoRegistry.getResourceDao(theId.getResourceType()).read(theId, requestDetails);
	}

	@Override
	public void update(IBaseResource theResource) {
		this.daoRegistry.getResourceDao(theResource.fhirType()).update(theResource, requestDetails);
	}

	@Override
	public void delete(IIdType theId) {
		this.daoRegistry.getResourceDao(theId.getResourceType()).delete(theId, requestDetails);

	}

	// TODO: the search interfaces need some work
	@Override
	public Iterable<IBaseResource> search(String theResourceType) {
		return this.daoRegistry.getResourceDao(theResourceType).search(SearchParameterMap.newSynchronous())
				.getAllResources();
	}

	@Override
	public Iterable<IBaseResource> searchByUrl(String theResourceType, String theUrl) {
		SearchParameterMap searchParameterMap = SearchParameterMap.newSynchronous();
		searchParameterMap.add("url", new UriParam(theUrl));
		Map<String, String> urlVersionManifestMap = (HashMap) requestDetails.getUserData().get("manifest");
		if (urlVersionManifestMap != null && urlVersionManifestMap.containsKey(theUrl)) {
			searchParameterMap.add("version", new TokenParam(urlVersionManifestMap.get(theUrl)));
		}
		return this.daoRegistry.getResourceDao(theResourceType)
			.search(searchParameterMap).getAllResources();
	}
}
