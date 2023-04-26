package org.opencds.cqf.ruler.cr;

import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Measure;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.UriParam;
import org.opencds.cqf.ruler.utility.Searches;

@SuppressWarnings("unchecked")
public class JpaCRFhirDal implements FhirDal {

	protected final DaoRegistry daoRegistry;
	protected final RequestDetails requestDetails;

	public JpaCRFhirDal(DaoRegistry daoRegistry) {
		this(daoRegistry, null);
	}

	public JpaCRFhirDal(DaoRegistry daoRegistry, RequestDetails requestDetails) {
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
		if (theUrl.contains("|")){
			var urlSplit = theUrl.split("\\|");
			var urlBase = urlSplit[0];
			var urlVersion = urlSplit[1];

			return this.daoRegistry.getResourceDao(theResourceType)
				.search(
					SearchParameterMap
						.newSynchronous()
						.add("url", new UriParam(urlBase))
						.add("version", new TokenParam(urlVersion))
				)
				.getAllResources();
		}
		else {
			return this.daoRegistry.getResourceDao(theResourceType)
				.search(
					SearchParameterMap
						.newSynchronous()
						.add("url", new UriParam(theUrl)))
				.getAllResources();
		}
	}
}
