package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.cqframework.fhir.api.FhirService;

@SuppressWarnings("unchecked")
public class JpaFhirPlatform implements org.cqframework.fhir.api.FhirPlatform {

    public ca.uhn.fhir.jpa.api.dao.DaoRegistry daoRegistry = null;
    public RequestDetails requestDetails = null;

    public JpaFhirPlatform() {}

    public JpaFhirPlatform(ca.uhn.fhir.jpa.api.dao.DaoRegistry daoRegistry, RequestDetails requestDetails) {
        this.daoRegistry = daoRegistry;
        this.requestDetails = requestDetails;
    }

    @Override
    public org.cqframework.fhir.api.FhirDal dal() {
        JpaFhirDal jpaFhirDal = new JpaFhirDal(daoRegistry, requestDetails);

        return jpaFhirDal;
    }

    @Override
    public org.cqframework.fhir.api.FhirCapabilities capabilities() {
        JpaFhirCapabilities jpaFhirCapabilities = new JpaFhirCapabilities(daoRegistry, requestDetails);
        return jpaFhirCapabilities;
    }

    @Override
    public org.cqframework.fhir.api.FhirTransactions transactions() {

        JpaFhirTransactions jpaFhirTransactions = new JpaFhirTransactions(daoRegistry, requestDetails);
        return jpaFhirTransactions;
    }

    @Override
    public FhirService getService() {

		 return null;
	 }
}
