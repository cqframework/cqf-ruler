package org.opencds.cqf.ruler.cql;

import ca.uhn.fhir.rest.api.server.RequestDetails;

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
        org.cqframework.fhir.api.JpaFhirDal fhirDal = new org.cqframework.fhir.api.JpaFhirDal();

        return fhirDal;
    }

    @Override
    public org.cqframework.fhir.api.FhirCapabilities capabilities() {

        org.cqframework.fhir.api.JpaFhirCapabilities jpaFhirCapabilities = new org.cqframework.fhir.api.JpaFhirCapabilities();
        return jpaFhirCapabilities;
    }

    @Override
    public org.cqframework.fhir.api.FhirTransactions transactions() {

        org.cqframework.fhir.api.JpaFhirTransactions jpaFhirTransactions = new org.cqframework.fhir.api.JpaFhirTransactions();
        return jpaFhirTransactions;
    }

    @Override
    public void getService() {

    }
}
