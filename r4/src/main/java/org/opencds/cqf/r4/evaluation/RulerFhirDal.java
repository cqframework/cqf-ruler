package org.opencds.cqf.r4.evaluation;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.UriParam;

public class RulerFhirDal implements FhirDal {

    protected DaoRegistry daoRegistry;

    public RulerFhirDal(DaoRegistry daoRegistry) {
        this.daoRegistry = daoRegistry;
    }

    @Override
    public IBaseResource read(IIdType id) {
        return this.daoRegistry.getResourceDao(id.getResourceType()).read(id);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void create(IBaseResource resource) {
        this.daoRegistry.getResourceDao(resource.fhirType()).create(resource);  
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update(IBaseResource resource) {
        this.daoRegistry.getResourceDao(resource.fhirType()).update(resource);
    }

    @Override
    public void delete(IIdType id) {
        this.daoRegistry.getResourceDao(id.getResourceType()).delete(id);
    }

    @Override
    public Iterable<IBaseResource> search(String resourceType) {
        return this.daoRegistry.getResourceDao(resourceType).search(SearchParameterMap.newSynchronous()).getAllResources();
    }

    @Override
    public Iterable<IBaseResource> searchByUrl(String resourceType, String url) {
        return this.daoRegistry.getResourceDao(resourceType).search(SearchParameterMap.newSynchronous().add("url", new UriParam(url))).getAllResources();
    }
}
