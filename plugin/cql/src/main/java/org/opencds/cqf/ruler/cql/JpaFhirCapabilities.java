package org.opencds.cqf.ruler.cql;

import org.hl7.fhir.instance.model.api.IBaseConformance;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IIdType;

@SuppressWarnings("unchecked")
public class JpaFhirCapabilities implements org.cqframework.fhir.api.FhirCapabilities {

    protected DaoRegistry daoRegistry = null;
    protected RequestDetails requestDetails = null;

    public JpaFhirCapabilities() {}

    public JpaFhirCapabilities(DaoRegistry daoRegistry) {
        this(daoRegistry, null);
    }

    public JpaFhirCapabilities(DaoRegistry daoRegistry, RequestDetails requestDetails) {
        this.daoRegistry = daoRegistry;
        this.requestDetails = requestDetails;
    }

    //@Override
    public IBaseConformance capabilities(IBaseConformance theConformance, IIdType theId) {
        return null; //this.daoRegistry.getResourceDao(theConformance.fhirType()).readEntity(theId, requestDetails).;
    }

    @Override
    public IBaseConformance capabilities() {
        return null;
    }
}
