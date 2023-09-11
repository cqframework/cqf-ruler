package org.opencds.cqf.ruler.cr;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.cqframework.fhir.api.FhirDal;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
				for (IQueryParameterType query : value) {
					searchParameterMap.add(keyValue, query);
				}
			}
		}

		List<IBaseResource> searchResults = this.daoRegistry.getResourceDao(theResourceType).search(searchParameterMap).getAllResources();

		ca.uhn.fhir.util.BundleBuilder builder = new ca.uhn.fhir.util.BundleBuilder(daoRegistry.getSystemDao().getContext());
		builder
			.setBundleField("type", "searchset")
			.setBundleField("id", UUID.randomUUID().toString())
			.setMetaField("lastUpdated", builder.newPrimitive("instant", new java.util.Date()));

		for (var resource : searchResults) {
			IBase entry = builder.addEntry();
			builder.addToEntry(entry, "resource", resource);

			// Add search results
			IBase search = builder.addSearch(entry);
			builder.setSearchField(search, "mode", "match");
			builder.setSearchField(search, "score", builder.newPrimitive("decimal", BigDecimal.ONE));
		}

		return builder.getBundle();
	}
}
