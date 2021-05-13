package org.opencds.cqf.ruler.common.dal;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.TokenParam;

public class RulerDal implements FhirDal {
    DaoRegistry registry;
    public RulerDal(DaoRegistry registry) {
        this.registry = registry;
    }

    @Override
    public IBaseResource read(IIdType iIdType) {
        return registry.getResourceDao(iIdType.getResourceType()).read(iIdType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void create(IBaseResource iBaseResource) {
        registry.getResourceDao(iBaseResource.fhirType()).create(iBaseResource);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void update(IBaseResource iBaseResource) {
        registry.getResourceDao(iBaseResource.fhirType()).update(iBaseResource);
    }

    @Override
    public void delete(IIdType iIdType) {
        registry.getResourceDao(iIdType.getResourceType()).delete(iIdType);
    }

    // TODO: update to getAllResources
    @Override
    public Iterable<IBaseResource> search(String resourceType) {
        return registry.getResourceDao(resourceType).search(SearchParameterMap.newSynchronous()).getResources(0, 10000);
    }

    // TODO: update to getAllResources
    @Override
    public Iterable<IBaseResource> searchByUrl(String resourceType, String url) {
        return registry.getResourceDao(resourceType).search(SearchParameterMap.newSynchronous().add("url", new TokenParam(url))).getResources(0, 10000);
    }
}
