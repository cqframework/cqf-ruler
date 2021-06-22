package org.opencds.cqf.dstu3.builders;

import org.hl7.fhir.dstu3.model.StructureMap;
import org.opencds.cqf.common.builders.BaseBuilder;

public class StructureMapGroupBuilder extends BaseBuilder<StructureMap.StructureMapGroupComponent> {

    public StructureMapGroupBuilder() {
        this(new StructureMap.StructureMapGroupComponent());
    }

    public StructureMapGroupBuilder(StructureMap.StructureMapGroupComponent complexProperty) {
        super(complexProperty);
    }

    public StructureMapGroupBuilder buildName(String name) {
        complexProperty.setName(name);
        return this;
    }

    public StructureMapGroupBuilder buildTypeMode(StructureMap.StructureMapGroupTypeMode typeMode) {
        complexProperty.setTypeMode(typeMode);
        return this;
    }

    public StructureMapGroupBuilder buildDocumentation(String description) {
        complexProperty.setDocumentation(description);
        return this;
    }

    public StructureMapGroupBuilder buildInputSource(String name) {
        StructureMap.StructureMapGroupInputComponent input = new StructureMap.StructureMapGroupInputComponent();
        input.setName(name);
        input.setMode(StructureMap.StructureMapInputMode.SOURCE);
        complexProperty.addInput(input);
        return this;
    }

    public StructureMapGroupBuilder buildInputTarget(String name, String fhirType) {
        StructureMap.StructureMapGroupInputComponent input = new StructureMap.StructureMapGroupInputComponent();
        input.setName(name);
        input.setMode(StructureMap.StructureMapInputMode.TARGET);
        input.setType(fhirType);
        complexProperty.addInput(input);
        return this;
    }

    public StructureMapGroupBuilder buildRule(
            StructureMap.StructureMapGroupRuleComponent structureMapGroupRuleComponent) {
        complexProperty.addRule(structureMapGroupRuleComponent);
        return this;
    }

}