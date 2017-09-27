package org.opencds.cqf.cds;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.MethodOutcome;
import org.hl7.fhir.dstu3.model.*;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.execution.Context;

import java.util.ArrayList;
import java.util.List;

public abstract class CdsRequestProcessor implements Processor {
    CdsHooksRequest request;
    PlanDefinition planDefinition;
    LibraryResourceProvider libraryResourceProvider;

    CdsRequestProcessor(CdsHooksRequest request, PlanDefinition planDefinition, LibraryResourceProvider libraryResourceProvider) {
        this.request = request;
        this.planDefinition = planDefinition;
        this.libraryResourceProvider = libraryResourceProvider;
    }

    List<CdsCard> resolveActions(Context executionContext) {
        List<CdsCard> cards = new ArrayList<>();

        walkAction(executionContext, cards, planDefinition.getAction());

        return cards;
    }

    private void walkAction(Context executionContext, List<CdsCard> cards, List<PlanDefinition.PlanDefinitionActionComponent> actions) {
        for (PlanDefinition.PlanDefinitionActionComponent action : actions) {
            boolean conditionsMet = true;
            for (PlanDefinition.PlanDefinitionActionConditionComponent condition: action.getCondition()) {
                if (condition.getKind() == PlanDefinition.ActionConditionKind.APPLICABILITY) {
                    if (!condition.hasExpression()) {
                        continue;
                    }

                    Object result = executionContext.resolveExpressionRef(condition.getExpression()).getExpression().evaluate(executionContext);

                    if (!(result instanceof Boolean)) {
                        continue;
                    }

                    if (!(Boolean) result) {
                        conditionsMet = false;
                    }
                }
            }
            if (conditionsMet) {

                /*
                    Cases:
                        Definition element provides guidance for action
                        Nested actions
                        Standardized CQL (when first 2 aren't present)
                */

                if (action.hasDefinition()) {
                    if (action.getDefinitionTarget().fhirType().equals("ActivityDefinition")) {
                        // ActivityDefinition $apply
                        BaseFhirDataProvider provider = (BaseFhirDataProvider) executionContext.resolveDataProvider("http://hl7.org/fhir");
                        Parameters inParams = new Parameters();
                        inParams.addParameter().setName("patient").setValue(new StringType(request.getPatientId()));

                        Parameters outParams = provider.getFhirClient()
                                .operation()
                                .onInstance(new IdDt("ActivityDefinition", action.getDefinition().getId()))
                                .named("$apply")
                                .withParameters(inParams)
                                .useHttpGet()
                                .execute();

                        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();
                        Resource resource = response.get(0).getResource();

                        if (resource != null) {
                            // put resulting resource into data provider
                            MethodOutcome outcome = provider.getFhirClient().create().resource(resource).execute();
                        }

                        // TODO - info card saying resource was created and provide the id?
                    }

                    else {
                        // PlanDefinition $apply
                        // TODO

                        // put CarePlan in provider

                        // TODO - info card saying CarePlan was created and provide the id?
                    }
                }

                else if (action.hasAction()) {
                    walkAction(executionContext, cards, action.getAction());
                }

                // TODO - dynamicValues

                else {
                    CdsCard card = new CdsCard();
                    card.setSummary((String) executionContext.resolveExpressionRef("getSummary").getExpression().evaluate(executionContext));
                    card.setDetail((String) executionContext.resolveExpressionRef("getDetail").getExpression().evaluate(executionContext));
                    card.setIndicator((String) executionContext.resolveExpressionRef("getIndicator").getExpression().evaluate(executionContext));
                    cards.add(card);
                }
            }
        }
    }
}
