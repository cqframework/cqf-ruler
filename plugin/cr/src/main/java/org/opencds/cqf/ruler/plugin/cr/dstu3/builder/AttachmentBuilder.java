package org.opencds.cqf.ruler.plugin.cr.dstu3.builder;

import java.util.List;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Extension;

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
