package org.opencds.cqf.ruler.server.factory;

import java.util.Set;

import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.builder.dal.FhirDalFactory;
import org.opencds.cqf.cql.evaluator.builder.dal.TypedFhirDalFactory;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

public class DefaultingFhirDalFactory extends FhirDalFactory {

    private FhirDal fhirDal;

    public DefaultingFhirDalFactory(Set<TypedFhirDalFactory> fhirDalFactories, FhirDal fhirDal) {
        super(fhirDalFactories);
        this.fhirDal = fhirDal;
    }

    @Override
    public FhirDal create(EndpointInfo endpointInfo) {
        if (endpointInfo == null) {
            return this.fhirDal;
        }

        return super.create(endpointInfo);
    }
}
