package org.opencds.cqf.r4.builders;

import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureMap;
import org.opencds.cqf.common.builders.BaseBuilder;

public class StructuredMapRuleSourceBuilder extends BaseBuilder<StructureMap.StructureMapGroupRuleSourceComponent> {

    public StructuredMapRuleSourceBuilder() {
        this(new StructureMap.StructureMapGroupRuleSourceComponent());
    }

    public StructuredMapRuleSourceBuilder(StructureMap.StructureMapGroupRuleSourceComponent complexProperty) {
        super(complexProperty);
    }

    public StructuredMapRuleSourceBuilder buildContext(String context) {
        complexProperty.setContext(context);
        return this;
    }

    public StructuredMapRuleSourceBuilder buildDefaultValue(String name) {
        complexProperty.setDefaultValue(new StringType(name));
        return this;
    }
}
