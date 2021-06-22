package org.opencds.cqf.dstu3.builders;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.common.builders.BaseBuilder;

public class ProcedureRequestBuilder extends BaseBuilder<ProcedureRequest> {

    // TODO - this is a start, but should be extended for completeness.

    public ProcedureRequestBuilder() {
        this(new ProcedureRequest());
    }

    public ProcedureRequestBuilder(ProcedureRequest complexProperty) {
        super(complexProperty);
    }

    public ProcedureRequestBuilder buildId(String id) {
        complexProperty.setId(id);
        return this;
    }

    public ProcedureRequestBuilder buildStatus(ProcedureRequest.ProcedureRequestStatus status) {
        complexProperty.setStatus(status);
        return this;
    }

    public ProcedureRequestBuilder buildStatus(String status) {
        try {
            complexProperty.setStatus(ProcedureRequest.ProcedureRequestStatus.fromCode(status));
        } catch (FHIRException e) {
            // default to null
            complexProperty.setStatus(ProcedureRequest.ProcedureRequestStatus.NULL);
        }
        return this;
    }

    public ProcedureRequestBuilder buildCode(Coding coding) {
        complexProperty.setCode(new CodeableConceptBuilder().buildCoding(coding).build());
        return this;
    }

    public ProcedureRequestBuilder buildIntent(ProcedureRequest.ProcedureRequestIntent intent) {
        complexProperty.setIntent(intent);
        return this;
    }

    public ProcedureRequestBuilder buildIntent(String intent) {
        try {
            complexProperty.setIntent(ProcedureRequest.ProcedureRequestIntent.fromCode(intent));
        } catch (FHIRException e) {
            // default to null
            complexProperty.setIntent(ProcedureRequest.ProcedureRequestIntent.NULL);
        }
        return this;
    }

    public ProcedureRequestBuilder buildPriority(ProcedureRequest.ProcedureRequestPriority priority) {
        complexProperty.setPriority(priority);
        return this;
    }

    public ProcedureRequestBuilder buildPriority(String priority) {
        try {
            complexProperty.setPriority(ProcedureRequest.ProcedureRequestPriority.fromCode(priority));
        } catch (FHIRException e) {
            // default to null
            complexProperty.setPriority(ProcedureRequest.ProcedureRequestPriority.NULL);
        }
        return this;
    }

    public ProcedureRequestBuilder buildSubject(String subject) {
        complexProperty.setSubject(new ReferenceBuilder().buildReference(subject).build());
        return this;
    }

    public ProcedureRequestBuilder buildRequester(String requester) {
        ProcedureRequest.ProcedureRequestRequesterComponent requestRequesterComponent = new ProcedureRequest.ProcedureRequestRequesterComponent();
        requestRequesterComponent.setAgent(new ReferenceBuilder().buildReference(requester).build());
        complexProperty.setRequester(requestRequesterComponent);
        return this;
    }

    public ProcedureRequestBuilder buildExtension(Extension extension) {
        complexProperty.addExtension(extension);
        return this;
    }
}
