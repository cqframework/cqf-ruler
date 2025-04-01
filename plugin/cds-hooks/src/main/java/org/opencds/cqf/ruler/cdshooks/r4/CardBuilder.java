package org.opencds.cqf.ruler.cdshooks.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.cr.r4.activitydefinition.ActivityDefinitionOperationsProvider;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.cdshooks.response.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CardBuilder {
	private final FhirContext fhirContext = FhirContext.forR4Cached();
	private final String patientId;
	private final Parameters evaluationResults;
	private final PlanDefinition planDefinition;
	private final ActivityDefinitionOperationsProvider applyEvaluator;
	private final ServletRequestDetails requestDetails;
	private final ModelResolver modelResolver;
	private final CqlExecutionHandler cqlExecutionHandler;

	public CardBuilder(String patientId, Parameters evaluationResults, PlanDefinition planDefinition, ActivityDefinitionOperationsProvider applyEvaluator, ServletRequestDetails requestDetails, ModelResolver modelResolver, CqlExecutionHandler cqlExecutionHandler) {
		this.patientId = patientId;
		this.evaluationResults = evaluationResults;
		this.planDefinition = planDefinition;
		this.applyEvaluator = applyEvaluator;
		this.requestDetails = requestDetails;
		this.modelResolver = modelResolver;
		this.cqlExecutionHandler = cqlExecutionHandler;
	}

	public List<Card> buildCards() {
		var planLinks = resolvePlanLinks();
		var cards = new ArrayList<Card>();

		if (planDefinition.hasAction()) {
			var topLevelCard = resolveServicePlan(
				planDefinition.getAction(),
				planLinks,
				true,
				null
			);
			if (topLevelCard != null) {
				cards.add(topLevelCard);
			}
		}
		return cards;
	}

	private List<Card.Link> resolvePlanLinks() {
		var links = new ArrayList<Card.Link>();
		if (planDefinition.hasRelatedArtifact()) {
			for (var ra : planDefinition.getRelatedArtifact()) {
				var link = new Card.Link();
				if (ra.hasDisplay()) {
					link.setLabel(ra.getDisplay());
				}
				if (ra.hasUrl()) {
					link.setUrl(ra.getUrl());
				}
				// Use "absolute" as default type if none provided
				link.setType(ra.hasExtension()
					? ra.getExtensionFirstRep().getValue().primitiveValue()
					: "absolute"
				);
				links.add(link);
			}
		}
		return links;
	}

	private Card resolveServicePlan(List<PlanDefinition.PlanDefinitionActionComponent> actions,
											  List<Card.Link> links, boolean newCard, Card oldCard) {
		var card = newCard ? new Card() : oldCard;

		for (var action : actions) {
			if (resolveCondition(action).get()) {
				processActionProperties(action, card);

				if (action.hasAction()) {
					resolveServicePlan(action.getAction(), links, false, card);
				}
				var extension = new Card.Extension();
				extension.setMimeType("text/html");
				card.setExtension(extension);
			}
		}

		// Only attach links if we have a summary
		if (card.getSummary() != null && links != null) {
			card.setLinks(links);
		} else if (card.getSummary() == null) {
			return null;
		}
		return card;
	}

	private void processActionProperties(PlanDefinition.PlanDefinitionActionComponent action, Card card) {
		// Title, description, priority => summary, detail, indicator
		if (action.hasTitle()) {
			card.setSummary(action.getTitle());
		}
		if (action.hasDescription()) {
			card.setDetail(action.getDescription());
		}
		if (action.hasPriority()) {
			String indicator = deriveIndicator(action.getPriority());
			card.setIndicator(indicator);
		}
		if (action.hasDocumentation()) {
			card.setSource(resolveSource(action));
		}
		if (action.hasSelectionBehavior()) {
			card.setSelectionBehavior(action.getSelectionBehavior().toCode());
		}
		if (action.hasDynamicValue()) {
			resolveDynamicActions(action, card);
		}
		if (action.hasReason()) {
			resolveOverrideReasons(action, card);
		}
		if (action.hasDefinition()) {
			var suggestion = resolveSuggestions(action);
			card.addSuggestion(suggestion);
		}
	}

	private String deriveIndicator(PlanDefinition.RequestPriority priority) {
		switch (priority.toCode()) {
			case "routine":
				return "info";
			case "urgent":
				return "warning";
			case "stat":
				return "critical";
			default:
				throw new IllegalArgumentException("Invalid priority code: " + priority.toCode());
		}
	}

	public AtomicBoolean resolveCondition(PlanDefinition.PlanDefinitionActionComponent action) {
		var conditionMet = new AtomicBoolean(false);
		if (!action.hasCondition()) {
			// If no conditions, assume true
			return new AtomicBoolean(true);
		}

		for (var condition : action.getCondition()) {
			if (condition.hasExpression() && condition.getExpression().hasLanguage() && condition.getExpression().hasExpression()) {
				var conditionResult = evaluateConditionExpression(condition);
				if (conditionResult != null) {
					conditionMet.set(conditionResult.isPrimitive() &&
						Boolean.parseBoolean(conditionResult.primitiveValue()));
				}
			}
		}
		return conditionMet;
	}

	public Card.Source resolveSource(PlanDefinition.PlanDefinitionActionComponent action) {
		var source = new Card.Source();
		var doc = action.getDocumentationFirstRep();
		if (doc.hasDisplay()) {
			source.setLabel(doc.getDisplay());
		}
		if (doc.hasUrl()) {
			source.setUri(doc.getUrl());
		}
		return source;
	}

	public Card.Suggestion resolveSuggestions(PlanDefinition.PlanDefinitionActionComponent action) {
		var suggestion = new Card.Suggestion();
		var suggAction = new Card.Suggestion.Action();
		suggAction.fhirContext = fhirContext;

		if (action.hasPrefix()) {
			suggestion.setLabel(action.getPrefix());
		}
		if (action.hasPrecheckBehavior()) {
			var isRecommended = action.getPrecheckBehavior().equals(PlanDefinition.ActionPrecheckBehavior.YES);
			suggestion.setIsRecommended(isRecommended);
		}

		boolean hasAction = false;
		if (action.hasDescription()) {
			suggAction.setDescription(action.getDescription());
			hasAction = true;
		}
		if (action.hasType() && action.getType().hasCoding()) {
			var actionCode = action.getType().getCodingFirstRep().getCode();
			// Skip "fire-event" in your logic
			if (actionCode != null && !"fire-event".equals(actionCode)) {
				suggAction.setType(actionCode);
				hasAction = true;
			}
		}

		if (action.hasDefinitionCanonicalType() &&
			action.getDefinitionCanonicalType().getValue().contains("ActivityDefinition")) {
			suggAction.setType("create");
			var definitionId = new IdType(
				Canonicals.getResourceType(action.getDefinitionCanonicalType().getValue()),
				Canonicals.getIdPart(action.getDefinitionCanonicalType().getValue())
			);
			var updatedPatientId = patientId.startsWith("Patient/") ? patientId : "Patient/" + patientId;
			var resource = applyEvaluator.apply(
				definitionId, updatedPatientId, null, updatedPatientId,
				null, null, null, null, null, null, null, null, null, null, null, null,
				requestDetails
			);
			suggAction.setResource(resource);
			hasAction = true;

			// Example hacky behavior for ServiceRequest
			if (resource instanceof ServiceRequest) {
				suggAction.setDescription("Service Request for Urine Drug Screening");
				((ServiceRequest) resource).setIntent(ServiceRequest.ServiceRequestIntent.PROPOSAL);
				var category = Collections.singletonList(
					new CodeableConcept().addCoding(
						new Coding()
							.setSystem("http://terminology.hl7.org/CodeSystem/medicationrequest-category")
							.setCode("outpatient")
							.setDisplay("Outpatient")
					)
				);
				((ServiceRequest) resource).setCategory(category);
			}
		}

		if (hasAction) {
			suggestion.setActions(Collections.singletonList(suggAction));
		}
		return suggestion;
	}

	public void resolveDynamicActions(PlanDefinition.PlanDefinitionActionComponent action, Card card) {
		for (var dv : action.getDynamicValue()) {
			if (dv.hasPath() && dv.hasExpression() && dv.getExpression().hasLanguage() && dv.getExpression().hasExpression()) {
				var dynamicValueResult = evaluateDynamicValue(dv);
				if (dynamicValueResult == null) {
					continue;
				}
				applyDynamicValueToCard(dv, card, dynamicValueResult);
			}
		}
	}

	private void applyDynamicValueToCard(PlanDefinition.PlanDefinitionActionDynamicValueComponent dv,
													 Card card, IBase dynamicValueResult) {
		var path = dv.getPath();
		var resultString = dynamicValueResult.toString();

		if (path.endsWith("title")) {
			card.setSummary(resultString);
		} else if (path.endsWith("description")) {
			card.setDetail(resultString);
			if (card.getSuggestions() != null && !card.getSuggestions().isEmpty()) {
				var actions = card.getSuggestions().get(0).getActions();
				if (actions != null && !actions.isEmpty()) {
					actions.get(0).setDescription(resultString);
				}
			}
		}
		// else set modelResolver
		else if (card.getSuggestions() != null && !card.getSuggestions().isEmpty()) {
			var firstSuggestion = card.getSuggestions().get(0);
			var actions = firstSuggestion.getActions();
			if (actions != null && !actions.isEmpty()) {
				var resource = actions.get(0).getResource();
				if (resource != null) {
					modelResolver.setValue(resource, path, dynamicValueResult);
				}
			}
		}
	}

	public void resolveOverrideReasons(PlanDefinition.PlanDefinitionActionComponent action, Card card) {
		if (action.hasReason()) {
			for (var reason : action.getReason()) {
				for (var coding : reason.getCoding()) {
					var overrideCoding = new Card.Coding();
					overrideCoding.setSystem(coding.getSystem());
					overrideCoding.setCode(coding.getCode());
					overrideCoding.setDisplay(coding.getDisplay());
					card.addOverrideReason(overrideCoding);
				}
			}
		}
	}

	private Type evaluateConditionExpression(PlanDefinition.PlanDefinitionActionConditionComponent condition) {
		var lang = condition.getExpression().getLanguage();
		var expressionText = condition.getExpression().getExpression();
		if ("text/cql-identifier".equals(lang) || "text/cql.identifier".equals(lang)) {
			return evaluationResults.getParameter(expressionText).getValue();
		} else if ("text/cql".equals(lang)) {
			return cqlExecutionHandler.evaluateExpressionGetResult(patientId, expressionText, null);
		}
		// Default => false
		return new BooleanType(false);
	}

	private IBase evaluateDynamicValue(PlanDefinition.PlanDefinitionActionDynamicValueComponent dv) {
		var lang = dv.getExpression().getLanguage();
		var expressionText = dv.getExpression().getExpression();
		if ("text/cql-identifier".equals(lang) || "text/cql.identifier".equals(lang)) {
			return evaluationResults.getParameter(expressionText).getValue();
		} else {
			return cqlExecutionHandler.evaluateExpressionGetResult(patientId, expressionText, null);
		}
	}
}
