package org.opencds.cqf.ruler.cr;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseBundle;

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
        return null;  //this.daoRegistry.getResourceDao(theTransaction.fhirType()).create(theTransaction, requestDetails);
    }
}
