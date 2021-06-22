package org.opencds.cqf.r4.builders;

import org.hl7.fhir.r4.model.StructureMap;
import org.opencds.cqf.common.builders.BaseBuilder;

public class StructuredMapRuleBuilder extends BaseBuilder<StructureMap.StructureMapGroupRuleComponent> {

    public StructuredMapRuleBuilder() {
        this(new StructureMap.StructureMapGroupRuleComponent());
    }

    public StructuredMapRuleBuilder(StructureMap.StructureMapGroupRuleComponent complexProperty) {
        super(complexProperty);
    }

    public StructuredMapRuleBuilder buildName(String name) {
        complexProperty.setName(name);
        return this;
    }

    public StructuredMapRuleBuilder buildSource(StructureMap.StructureMapGroupRuleSourceComponent source) {
        complexProperty.addSource(source);
        return this;
    }

    public StructuredMapRuleBuilder buildTarget(StructureMap.StructureMapGroupRuleTargetComponent target) {
        complexProperty.addTarget(target);
        return this;
    }

    public StructuredMapRuleBuilder buildTargetSetValue(String target, String targetField, String value) {
        return buildTarget(
                new StructuredMapRuleTargetBuilder().buildTransformSetValue(target, targetField, value).build());
    }

    public StructuredMapRuleBuilder buildTarget(String target, String targetField,
            StructureMap.StructureMapTransform transform, String... params) {
        return buildTarget(
                new StructuredMapRuleTargetBuilder().buildTransform(target, targetField, transform, params).build());
    }

}
