package org.opencds.cqf.builders;

import org.hl7.fhir.dstu3.model.Attachment;

public class AttachmentBuilder extends BaseBuilder<Attachment> {

    public AttachmentBuilder() {
        super(new Attachment());
    }

    // TODO - incomplete

    public AttachmentBuilder buildUrl(String url) {
        complexProperty.setUrl(url);
        return this;
    }
}
