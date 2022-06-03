package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

@SuppressWarnings("unchecked")
public class JpaFhirTransactions implements org.cqframework.fhir.api.FhirTransactions {

    public ca.uhn.fhir.jpa.api.dao.DaoRegistry daoRegistry;
    public RequestDetails requestDetails;

    public JpaFhirTransactions() {}

    public JpaFhirTransactions(ca.uhn.fhir.jpa.api.dao.DaoRegistry daoRegistry, RequestDetails requestDetails) {
        this.daoRegistry = daoRegistry;
        this.requestDetails = requestDetails;
    }

    @Override
    public IBaseBundle transaction(IBaseBundle theTransaction) {
        //return this.daoRegistry.getResourceDao(theTransaction.fhirType());
		  return null;
    }
}
