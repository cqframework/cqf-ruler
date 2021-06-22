package org.opencds.cqf.r4.builders;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.opencds.cqf.common.builders.BaseBuilder;

public class ServiceRequestBuilder extends BaseBuilder<ServiceRequest> {

    // TODO - this is a start, but should be extended for completeness.

    public ServiceRequestBuilder() {
        this(new ServiceRequest());
    }

    public ServiceRequestBuilder(ServiceRequest complexProperty) {
        super(complexProperty);
    }

    public ServiceRequestBuilder buildId(String id) {
        complexProperty.setId(id);
        return this;
    }

    public ServiceRequestBuilder buildStatus(ServiceRequest.ServiceRequestStatus status) {
        complexProperty.setStatus(status);
        return this;
    }

    public ServiceRequestBuilder buildStatus(String status) {
        try {
            complexProperty.setStatus(ServiceRequest.ServiceRequestStatus.fromCode(status));
        } catch (FHIRException e) {
            // default to null
            complexProperty.setStatus(ServiceRequest.ServiceRequestStatus.NULL);
        }
        return this;
    }

    public ServiceRequestBuilder buildCode(Coding coding) {
        complexProperty.setCode(new CodeableConceptBuilder().buildCoding(coding).build());
        return this;
    }

    public ServiceRequestBuilder buildIntent(ServiceRequest.ServiceRequestIntent intent) {
        complexProperty.setIntent(intent);
        return this;
    }

    public ServiceRequestBuilder buildIntent(String intent) {
        try {
            complexProperty.setIntent(ServiceRequest.ServiceRequestIntent.fromCode(intent));
        } catch (FHIRException e) {
            // default to null
            complexProperty.setIntent(ServiceRequest.ServiceRequestIntent.NULL);
        }
        return this;
    }

    public ServiceRequestBuilder buildPriority(ServiceRequest.ServiceRequestPriority priority) {
        complexProperty.setPriority(priority);
        return this;
    }

    public ServiceRequestBuilder buildPriority(String priority) {
        try {
            complexProperty.setPriority(ServiceRequest.ServiceRequestPriority.fromCode(priority));
        } catch (FHIRException e) {
            // default to null
            complexProperty.setPriority(ServiceRequest.ServiceRequestPriority.NULL);
        }
        return this;
    }

    public ServiceRequestBuilder buildSubject(String subject) {
        complexProperty.setSubject(new ReferenceBuilder().buildReference(subject).build());
        return this;
    }

    public ServiceRequestBuilder buildRequester(String requester) {
        complexProperty.setRequester(new Reference(requester));
        return this;
    }

    public ServiceRequestBuilder buildExtension(Extension extension) {
        complexProperty.addExtension(extension);
        return this;
    }
}
