package org.opencds.cqf.ruler.plugin.cdshooks.builders.r4;

import org.opencds.cqf.ruler.plugin.cdshooks.builders.BaseBuilder;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

public class ExtensionBuilder extends BaseBuilder<Extension> {

    public ExtensionBuilder() {
        super(new Extension());
    }

    public ExtensionBuilder buildUrl(String url) {
        complexProperty.setUrl(url);
        return this;
    }

    public ExtensionBuilder buildValue(Type value) {
        complexProperty.setValue(value);
        return this;
    }

    public ExtensionBuilder buildValue(String value) {
        complexProperty.setValue(new StringType(value));
        return this;
    }
}
