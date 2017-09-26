package org.opencds.cqf.cds;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.hl7.fhir.dstu3.model.PlanDefinition;
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

        for (PlanDefinition.PlanDefinitionActionComponent action : planDefinition.getAction()) {
            boolean conditionsMet = true;
            for (PlanDefinition.PlanDefinitionActionConditionComponent condition: action.getCondition()) {
                if (condition.getKind() == PlanDefinition.ActionConditionKind.APPLICABILITY) {
                    if (!condition.getLanguage().equals("text/cql")) {
                        continue;
                    }

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
                CdsCard card = new CdsCard();
                card.setSummary((String) executionContext.resolveExpressionRef("getSummary").getExpression().evaluate(executionContext));
                card.setDetail((String) executionContext.resolveExpressionRef("getDetail").getExpression().evaluate(executionContext));
                card.setIndicator((String) executionContext.resolveExpressionRef("getIndicator").getExpression().evaluate(executionContext));
                cards.add(card);
            }
        }

        return cards;
    }
}
