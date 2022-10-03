package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.model.api.IQueryParameterType;
//import ca.uhn.fhir.rest.api.server.IBundleProvider;
//import org.hl7.fhir.BundleEntry;
import ca.uhn.fhir.rest.param.DateParam;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.cqframework.fhir.api.FhirDal;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.builder.BundleBuilder;
//import org.opencds.cqf.ruler.utility.Operations;

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
	public IBaseBundle search(String theResourceType, Map<String, List<List<IQueryParameterType>>> theSearchParameters) {
		SearchParameterMap searchParameterMap = new SearchParameterMap();
		for(Map.Entry<String, List<List<IQueryParameterType>>> entry : theSearchParameters.entrySet()) {
			String keyValue = entry.getKey();

			for(List<IQueryParameterType> value : entry.getValue()) {
				searchParameterMap.add(keyValue, (DateParam) value);
			}
		}

		List<IBaseResource> searchResults = this.daoRegistry.getResourceDao(theResourceType).search(searchParameterMap).getAllResources();
		Bundle searchResultsBundle = new BundleBuilder<>(Bundle.class)
			.withType(Bundle.BundleType.COLLECTION.toString())
			.build();
		searchResults.forEach(result -> searchResultsBundle.addEntry(new Bundle.BundleEntryComponent().setResource((Resource)result)));
		return searchResultsBundle;
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
