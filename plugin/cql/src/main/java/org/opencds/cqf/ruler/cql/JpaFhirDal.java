package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.model.api.IQueryParameterType;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.cqframework.fhir.api.FhirDal;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.RequestDetails;

import java.util.List;
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

	@Override
	public IBaseBundle search(String theResourceType, Map<String, List<IQueryParameterType>> theSearchParameters) {
		SearchParameterMap searchParameterMap = new SearchParameterMap();
		for(Map.Entry<String, List<IQueryParameterType>> entry : theSearchParameters.entrySet()) {
			String keyValue = entry.getKey();
			for(IQueryParameterType value : entry.getValue()) {
				searchParameterMap.add(keyValue, value);
			}
		}

		return (IBaseBundle) this.daoRegistry.getResourceDao(theResourceType).search(searchParameterMap).getAllResources();
	}

	// TODO: the search interfaces need some work
	//@Override
	//public Iterable<IBaseResource> search(String theResourceType) {
	//	return this.daoRegistry.getResourceDao(theResourceType).search(SearchParameterMap.newSynchronous())
	//			.getAllResources();
	//}

	//@Override
	//public Iterable<IBaseResource> searchByUrl(String theResourceType, String theUrl) {
	//	return this.daoRegistry.getResourceDao(theResourceType)
	//			.search(SearchParameterMap.newSynchronous().add("url", new UriParam(theUrl))).getAllResources();
	//}
}
