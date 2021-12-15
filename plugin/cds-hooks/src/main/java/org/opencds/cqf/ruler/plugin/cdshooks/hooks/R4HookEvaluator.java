package org.opencds.cqf.ruler.plugin.cdshooks.hooks;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.api.IGenericClient;

import org.opencds.cqf.ruler.plugin.cdshooks.response.CdsCard;
import org.opencds.cqf.ruler.plugin.cdshooks.response.R4CarePlanToCdsCard;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.ruler.plugin.cdshooks.builders.r4.*;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class R4HookEvaluator extends BaseHookEvaluator<PlanDefinition> {

    public R4HookEvaluator(ModelResolver modelResolver) {
        super(modelResolver);
    }

    @Override
    public List<CdsCard> evaluateCdsHooksPlanDefinition(Context context, PlanDefinition planDefinition,
            String patientId, IGenericClient applyClient) {
        CarePlanBuilder carePlanBuilder = new CarePlanBuilder();
        RequestGroupBuilder requestGroupBuilder = new RequestGroupBuilder().buildStatus().buildIntent();

        // links
        if (planDefinition.hasRelatedArtifact()) {
            List<Extension> extensions = new ArrayList<>();
            for (RelatedArtifact relatedArtifact : planDefinition.getRelatedArtifact()) {
                AttachmentBuilder attachmentBuilder = new AttachmentBuilder();
                ExtensionBuilder extensionBuilder = new ExtensionBuilder();
                if (relatedArtifact.hasDisplay()) { // label
                    attachmentBuilder.buildTitle(relatedArtifact.getDisplay());
                }
                if (relatedArtifact.hasUrl()) { // url
                    attachmentBuilder.buildUrl(relatedArtifact.getUrl());
                }
                if (relatedArtifact.hasExtension()) { // type
                    attachmentBuilder.buildExtension(relatedArtifact.getExtension());
                }
                extensionBuilder.buildUrl("http://example.org");
                extensionBuilder.buildValue(attachmentBuilder.build());
                extensions.add(extensionBuilder.build());
            }
            requestGroupBuilder.buildExtension(extensions);
        }

        resolveActions(planDefinition.getAction(), context, patientId, requestGroupBuilder, new ArrayList<>(),
                applyClient);

        CarePlanActivityBuilder carePlanActivityBuilder = new CarePlanActivityBuilder();
        carePlanActivityBuilder.buildReferenceTarget(requestGroupBuilder.build());
        carePlanBuilder.buildActivity(carePlanActivityBuilder.build());

        return R4CarePlanToCdsCard.convert(carePlanBuilder.build());
    }

    private void resolveActions(List<PlanDefinition.PlanDefinitionActionComponent> actions, Context context,
            String patientId, RequestGroupBuilder requestGroupBuilder,
            List<RequestGroup.RequestGroupActionComponent> actionComponents, IGenericClient applyClient) {
        for (PlanDefinition.PlanDefinitionActionComponent action : actions) {
            boolean conditionsMet = true;
            for (PlanDefinition.PlanDefinitionActionConditionComponent condition : action.getCondition()) {
                if (condition.getKind() == PlanDefinition.ActionConditionKind.APPLICABILITY) {
                    if (!condition.hasExpression()) {
                        continue;
                    }

                    Object result = context.resolveExpressionRef(condition.getExpression().getExpression())
                            .getExpression().evaluate(context);

                    if (!(result instanceof Boolean)) {
                        continue;
                    }

                    if (!(Boolean) result) {
                        conditionsMet = false;
                    }
                }

                if (conditionsMet) {
                    RequestGroupActionBuilder actionBuilder = new RequestGroupActionBuilder();
                    if (action.hasTitle()) {
                        actionBuilder.buildTitle(action.getTitle());
                    }
                    if (action.hasDescription()) {
                        actionBuilder.buildDescripition(action.getDescription());
                    }

                    // source
                    if (action.hasDocumentation()) {
                        RelatedArtifact artifact = action.getDocumentationFirstRep();
                        RelatedArtifactBuilder artifactBuilder = new RelatedArtifactBuilder();
                        if (artifact.hasDisplay()) {
                            artifactBuilder.buildDisplay(artifact.getDisplay());
                        }
                        if (artifact.hasUrl()) {
                            artifactBuilder.buildUrl(artifact.getUrl());
                        }
                        if (artifact.hasDocument() && artifact.getDocument().hasUrl()) {
                            AttachmentBuilder attachmentBuilder = new AttachmentBuilder();
                            attachmentBuilder.buildUrl(artifact.getDocument().getUrl());
                            artifactBuilder.buildDocument(attachmentBuilder.build());
                        }
                        actionBuilder.buildDocumentation(Collections.singletonList(artifactBuilder.build()));
                    }

                    // suggestions
                    // TODO - uuid
                    if (action.hasPrefix()) {
                        actionBuilder.buildPrefix(action.getPrefix());
                    }
                    if (action.hasType()) {
                        actionBuilder.buildType(action.getType());
                    }
                    if (action.hasSelectionBehavior()) {
                        actionBuilder.buildSelectionBehavior(RequestGroup.ActionSelectionBehavior.fromCode(action.getSelectionBehavior().toCode()));
                    }

                    Resource resource = null;
                    if (action.hasDefinition()) {
                        if (action.getDefinitionCanonicalType().getValue().contains("ActivityDefinition")) {
                            Parameters inParams = new Parameters();
                            inParams.addParameter().setName("patient").setValue(new StringType(patientId));
                            Parameters outParams = applyClient.operation()
                                    .onInstance(new IdDt(action.getDefinitionCanonicalType().getValue()))
                                    .named("$apply").withParameters(inParams).useHttpGet().execute();

                            List<Parameters.ParametersParameterComponent> response = outParams.getParameter();
                            resource = response.get(0).getResource().setId(UUID.randomUUID().toString());
                        }
                    }

                    // Dynamic values populate the RequestGroup - there is a bit of hijacking going
                    // on here...
                    if (action.hasDynamicValue()) {
                        for (PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue : action
                                .getDynamicValue()) {
                            if (dynamicValue.hasPath() && dynamicValue.hasExpression()) {
                                if (dynamicValue.getPath().endsWith("title")) { // summary
                                    String title = (String) context
                                            .resolveExpressionRef(dynamicValue.getExpression().getExpression())
                                            .evaluate(context);
                                    actionBuilder.buildTitle(title);
                                } else if (dynamicValue.getPath().endsWith("description")) { // detail
                                    String description = (String) context
                                            .resolveExpressionRef(dynamicValue.getExpression().getExpression())
                                            .evaluate(context);
                                    actionBuilder.buildDescripition(description);
                                } else if (dynamicValue.getPath().endsWith("extension")) { // indicator
                                    String extension = (String) context
                                            .resolveExpressionRef(dynamicValue.getExpression().getExpression())
                                            .evaluate(context);
                                    actionBuilder.buildExtension(extension);
                                } else {
                                    if (resource != null) {
                                        Object value = (Object) context.resolveExpressionRef(dynamicValue.getExpression().getExpression()).evaluate(context);

                                        // TODO need to verify type... yay
                                        if (value instanceof Boolean) {
                                            value = new BooleanType((Boolean) value);
                                        }

                                        R4FhirModelResolver modelResolver = new R4FhirModelResolver();
                                        modelResolver.setValue(resource, dynamicValue.getPath(), value);

                                        actionBuilder.buildResourceTarget(resource);
                                        actionBuilder.buildResource(new ReferenceBuilder().buildReference(resource.getId()).build());
                                    }
                                }
                            }
                        }
                    }

                    if (!actionBuilder.build().isEmpty()) {
                        actionComponents.add(actionBuilder.build());
                    }

                    if (action.hasAction()) {
                        resolveActions(action.getAction(), context, patientId, requestGroupBuilder, actionComponents,
                                applyClient);
                    }
                }
            }
        }
        requestGroupBuilder.buildAction(new ArrayList<>(actionComponents));
    }
}
