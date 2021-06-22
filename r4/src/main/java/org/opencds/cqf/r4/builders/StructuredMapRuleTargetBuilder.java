package org.opencds.cqf.r4.builders;

import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureMap;
import org.opencds.cqf.common.builders.BaseBuilder;

public class StructuredMapRuleTargetBuilder extends BaseBuilder<StructureMap.StructureMapGroupRuleTargetComponent> {

    public StructuredMapRuleTargetBuilder() {
        this(new StructureMap.StructureMapGroupRuleTargetComponent());
    }

    public StructuredMapRuleTargetBuilder(StructureMap.StructureMapGroupRuleTargetComponent complexProperty) {
        super(complexProperty);
    }

    public StructuredMapRuleTargetBuilder buildContext(String context) {
        complexProperty.setContext(context);
        return this;
    }

    public StructuredMapRuleTargetBuilder buildContextType(StructureMap.StructureMapContextType contextType) {
        complexProperty.setContextType(contextType);
        return this;
    }

    public StructuredMapRuleTargetBuilder buildElement(String status) {
        complexProperty.setElement(status);
        return this;
    }

    public StructuredMapRuleTargetBuilder buildTransformCopy(String source) {
        complexProperty.setTransform(StructureMap.StructureMapTransform.COPY);
        StructureMap.StructureMapGroupRuleTargetParameterComponent structureMapGroupRuleTargetParameterComponent = new StructureMap.StructureMapGroupRuleTargetParameterComponent();
        structureMapGroupRuleTargetParameterComponent.setValue(new StringType(source));
        complexProperty.addParameter(structureMapGroupRuleTargetParameterComponent);
        return this;
    }

    public StructuredMapRuleTargetBuilder buildTransformSetValue(String context, String targetField, String value) {
        return buildContext(context).buildContextType(StructureMap.StructureMapContextType.VARIABLE)
                .buildElement(targetField).buildTransformCopy(value);
    }

    public StructuredMapRuleTargetBuilder buildTransformSetUuid(String context, String targetField) {
        complexProperty.setTransform(StructureMap.StructureMapTransform.UUID);
        return buildContext(context).buildContextType(StructureMap.StructureMapContextType.VARIABLE)
                .buildElement(targetField);
    }

    public StructuredMapRuleTargetBuilder buildTransform(String context, String targetField,
            StructureMap.StructureMapTransform transform, String... parameters) {
        complexProperty.setTransform(transform);
        for (String param : parameters) {
            StructureMap.StructureMapGroupRuleTargetParameterComponent structureMapGroupRuleTargetParameterComponent = new StructureMap.StructureMapGroupRuleTargetParameterComponent();
            structureMapGroupRuleTargetParameterComponent.setValue(new StringType(param));
            complexProperty.addParameter(structureMapGroupRuleTargetParameterComponent);
        }

        return buildContext(context).buildContextType(StructureMap.StructureMapContextType.VARIABLE)
                .buildElement(targetField);
    }
}
