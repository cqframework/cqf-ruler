package org.opencds.cqf.r4.builders;

import java.util.List;

import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.common.builders.BaseBuilder;

public class ValueSetIncludesBuilder extends BaseBuilder<ValueSet.ConceptSetComponent> {

    public ValueSetIncludesBuilder(ValueSet.ConceptSetComponent complexProperty) {
        super(complexProperty);
    }

    public ValueSetIncludesBuilder buildSystem(String system) {
        complexProperty.setSystem(system);
        return this;
    }

    public ValueSetIncludesBuilder buildVersion(String version) {
        complexProperty.setVersion(version);
        return this;
    }

    public ValueSetIncludesBuilder buildConcept(List<ValueSet.ConceptReferenceComponent> concepts) {
        complexProperty.setConcept(concepts);
        return this;
    }

    public ValueSetIncludesBuilder buildConcept(ValueSet.ConceptReferenceComponent concept) {
        complexProperty.addConcept(concept);
        return this;
    }

    public ValueSetIncludesBuilder buildConcept(String code, String display) {
        complexProperty.addConcept(new ValueSet.ConceptReferenceComponent().setCode(code).setDisplay(display));
        return this;
    }
}
