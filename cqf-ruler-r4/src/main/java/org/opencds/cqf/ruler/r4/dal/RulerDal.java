package org.opencds.cqf.ruler.r4.dal;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

public class RulerDal implements FhirDal {
    @Override
    public IBaseResource read(IIdType iIdType) {
        return null;
    }

    @Override
    public void create(IBaseResource iBaseResource) {

    }

    @Override
    public void update(IBaseResource iBaseResource) {

    }

    @Override
    public void delete(IIdType iIdType) {

    }

    @Override
    public Iterable<IBaseResource> search(String s) {
        return null;
    }

    @Override
    public Iterable<IBaseResource> searchByUrl(String s, String s1) {
        return null;
    }
}
