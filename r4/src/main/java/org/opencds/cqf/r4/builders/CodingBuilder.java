package org.opencds.cqf.r4.builders;

import org.hl7.fhir.r4.model.Coding;
import org.opencds.cqf.common.builders.BaseBuilder;

public class CodingBuilder extends BaseBuilder<Coding> {

    public CodingBuilder() {
        super(new Coding());
    }

    public CodingBuilder buildSystem(String system) {
        complexProperty.setSystem(system);
        return this;
    }

    public CodingBuilder buildVersion(String version) {
        complexProperty.setVersion(version);
        return this;
    }

    public CodingBuilder buildCode(String code) {
        complexProperty.setCode(code);
        return this;
    }

    public CodingBuilder buildCode(String system, String code, String display) {
        complexProperty.setSystem(system);
        complexProperty.setCode(code);
        complexProperty.setDisplay(display);
        return this;
    }

    public CodingBuilder buildCode(String system, String code) {
        complexProperty.setSystem(system);
        complexProperty.setCode(code);
        return this;
    }

    public CodingBuilder buildDisplay(String display) {
        complexProperty.setDisplay(display);
        return this;
    }

    public CodingBuilder buildUserSelected(boolean selected) {
        complexProperty.setUserSelected(selected);
        return this;
    }
}
