package org.opencds.cqf.r4.builders;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Expression;
import org.opencds.cqf.common.builders.BaseBuilder;

public class ActivityDefinitionBuilder extends BaseBuilder<ActivityDefinition> {

    // TODO - this is a start, but should be extended for completeness.

    public ActivityDefinitionBuilder() {
        super(new ActivityDefinition());
    }

    public ActivityDefinitionBuilder(ActivityDefinition activityDefinition) {
        super(activityDefinition);
    }

    public ActivityDefinitionBuilder buildIdentification(String url, String version,
            Enumerations.PublicationStatus status) {
        complexProperty.setUrl(url);
        complexProperty.setVersion(version);
        complexProperty.setStatus(status);

        return this;
    }

    public ActivityDefinitionBuilder buildId(String id) {
        complexProperty.setId(id);
        return this;
    }

    public ActivityDefinitionBuilder buildName(String name) {
        complexProperty.setName(name);
        return this;
    }

    public ActivityDefinitionBuilder buildTitle(String title) {
        complexProperty.setTitle(title);
        return this;
    }

    public ActivityDefinitionBuilder buildNameAndTitle(String title) {
        complexProperty.setTitle(title);
        complexProperty.setName(title);
        return this;
    }

    public ActivityDefinitionBuilder buildDescription(String description) {
        complexProperty.setDescription(description);
        return this;
    }

    public ActivityDefinitionBuilder buildCopyright(String copyright) {
        complexProperty.setCopyright(copyright);
        return this;
    }

    public ActivityDefinitionBuilder buildKind(ActivityDefinition.ActivityDefinitionKind activityDefinitionKind) {
        complexProperty.setKind(activityDefinitionKind);
        return this;
    }

    public ActivityDefinitionBuilder buildCode(Coding coding) {
        complexProperty.getCode().addCoding(coding);
        return this;
    }

    public ActivityDefinitionBuilder buildBodySite(Coding coding) {
        complexProperty.getBodySite().add(new CodeableConceptBuilder().buildCoding(coding).build());
        return this;
    }

    public ActivityDefinitionBuilder buildDynamicValue(String description, String path, String language,
            String expression) {
        ActivityDefinition.ActivityDefinitionDynamicValueComponent dynamicValueComponent = new ActivityDefinition.ActivityDefinitionDynamicValueComponent();
        dynamicValueComponent.setPath(path);
        dynamicValueComponent.setExpression(new Expression().setDescription(description)
                .setLanguage(Expression.ExpressionLanguage.fromCode(language).toCode()).setExpression(expression));
        complexProperty.addDynamicValue(dynamicValueComponent);
        return this;
    }

    public ActivityDefinitionBuilder buildCqlDynamicValue(String description, String path, String expression) {
        return this.buildDynamicValue(description, path, "text/cql", expression);
    }
}
