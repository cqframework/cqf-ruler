package org.opencds.cqf.dstu3.builders;

import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Reference;
import org.opencds.cqf.common.builders.BaseBuilder;

public class ReferenceBuilder extends BaseBuilder<Reference> {

    public ReferenceBuilder() {
        super(new Reference());
    }

    public ReferenceBuilder buildReference(String reference) {
        complexProperty.setReference(reference);
        return this;
    }

    public ReferenceBuilder buildIdentifier(Identifier identifier) {
        complexProperty.setIdentifier(identifier);
        return this;
    }

    public ReferenceBuilder buildDisplay(String display) {
        complexProperty.setDisplay(display);
        return this;
    }
}
