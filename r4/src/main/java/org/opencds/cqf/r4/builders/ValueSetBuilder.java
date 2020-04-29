package org.opencds.cqf.r4.builders;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.common.builders.BaseBuilder;

public class ValueSetBuilder extends BaseBuilder<ValueSet> {

    public ValueSetBuilder(ValueSet complexProperty) {
        super(complexProperty);
    }

    public ValueSetBuilder buildId(String id) {
        complexProperty.setId(id);
        return this;
    }

    public ValueSetBuilder buildUrl(String url) {
        complexProperty.setUrl(url);
        return this;
    }

    public ValueSetBuilder buildTitle(String title) {
        complexProperty.setTitle(title);
        return this;
    }

    public ValueSetBuilder buildStatus() {
        complexProperty.setStatus(Enumerations.PublicationStatus.DRAFT);
        return this;
    }

    public ValueSetBuilder buildStatus(String status) throws FHIRException {
        complexProperty.setStatus(Enumerations.PublicationStatus.fromCode(status));
        return this;
    }

    public ValueSetBuilder buildStatus(Enumerations.PublicationStatus status) {
        complexProperty.setStatus(status);
        return this;
    }

    public ValueSetBuilder buildCompose(ValueSet.ValueSetComposeComponent compose) {
        complexProperty.setCompose(compose);
        return this;
    }
}
