package org.opencds.cqf.r4.builders;

import org.opencds.cqf.common.builders.BaseBuilder;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Extension;

import java.util.List;

public class AttachmentBuilder extends BaseBuilder<Attachment> {

    public AttachmentBuilder() {
        super(new Attachment());
    }

    // TODO - incomplete

    public AttachmentBuilder buildUrl(String url) {
        complexProperty.setUrl(url);
        return this;
    }

    public AttachmentBuilder buildTitle(String title) {
        complexProperty.setTitle(title);
        return this;
    }

    public AttachmentBuilder buildExtension(List<Extension> extensions) {
        complexProperty.setExtension(extensions);
        return this;
    }
}
