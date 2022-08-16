package org.opencds.cqf.ruler.cdshooks.r4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import org.apache.http.entity.ContentType;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.exception.CqlException;
import org.opencds.cqf.cql.engine.exception.DataProviderException;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.cdshooks.CdsServicesCache;
import org.opencds.cqf.ruler.cdshooks.providers.ProviderConfiguration;
import org.opencds.cqf.ruler.cdshooks.request.CdsHooksRequest;
import org.opencds.cqf.ruler.cdshooks.response.Card;
import org.opencds.cqf.ruler.cdshooks.response.Cards;
import org.opencds.cqf.ruler.cdshooks.response.ErrorHandling;
import org.opencds.cqf.ruler.cpg.r4.provider.CqlExecutionProvider;
import org.opencds.cqf.ruler.cpg.r4.provider.LibraryEvaluationProvider;
import org.opencds.cqf.ruler.cql.CqlProperties;
import org.opencds.cqf.ruler.cr.r4.provider.ActivityDefinitionApplyProvider;
import org.opencds.cqf.ruler.external.AppProperties;
import org.opencds.cqf.ruler.utility.Canonicals;
import org.opencds.cqf.ruler.utility.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable
public class CdsHooksServlet extends HttpServlet implements DaoRegistryUser {
	private static final Logger logger = LoggerFactory.getLogger(CdsHooksServlet.class);
	private static final long serialVersionUID = 1L;

	@Autowired
	private CqlProperties cqlProperties;
	@Autowired
	private DaoRegistry daoRegistry;
	@Autowired
	private AppProperties myAppProperties;
	@Autowired
	private CqlExecutionProvider cqlExecution;
	@Autowired
	private LibraryEvaluationProvider libraryExecution;
	@Autowired
	private ActivityDefinitionApplyProvider applyEvaluator;
	@Autowired
	private ProviderConfiguration providerConfiguration;
	@Autowired
	private ModelResolver modelResolver;
	@Autowired
	CdsServicesCache cdsServicesCache;

	protected ProviderConfiguration getProviderConfiguration() {
		return this.providerConfiguration;
	}
	private final SystemRequestDetails requestDetails = new SystemRequestDetails();
	private R4CqlExecution cqlExecutor;

	// CORS Pre-flight
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		ErrorHandling.setAccessControlHeaders(resp, myAppProperties);
		resp.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		resp.setHeader("X-Content-Type-Options", "nosniff");
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		logger.info(request.getRequestURI());
		if (!request.getRequestURL().toString().endsWith("/cds-services")
				&& !request.getRequestURL().toString().endsWith("/cds-services/")) {
			logger.error(request.getRequestURI());
			throw new ServletException("This servlet is not configured to handle GET requests.");
		}
		ErrorHandling.setAccessControlHeaders(response, myAppProperties);
		response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		response.getWriter().println(new GsonBuilder().setPrettyPrinting().create().toJson(getServices()));
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			if (request.getContentType() == null || !request.getContentType().startsWith("application/json")) {
				throw new ServletException(String.format("Invalid content type %s. Please use application/json.",
						request.getContentType()));
			}
			logger.info(request.getRequestURI());
			String baseUrl = myAppProperties.getServer_address();
			String service = request.getPathInfo().replace("/", "");
			ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

			String requestJson = request.getReader().lines().collect(Collectors.joining());
			CdsHooksRequest cdsHooksRequest = mapper.readValue(requestJson, CdsHooksRequest.class);
			logRequestInfo(cdsHooksRequest, requestJson);

			cqlExecutor = new R4CqlExecution(baseUrl);
			requestDetails.setFhirServerBase(baseUrl);

			PlanDefinition servicePlan = read(Ids.newId(PlanDefinition.class, service));
			if (!servicePlan.hasLibrary()) {
				throw new ErrorHandling.CdsHooksError(
						"Logic library reference missing from PlanDefinition: " + servicePlan.getId());
			}

			IdType logicId = Ids.newId(Library.class, Canonicals.getIdPart(servicePlan.getLibrary().get(0)));

			String patientId;
			Parameters parameters = null;
			if (cdsHooksRequest instanceof CdsHooksRequest.OrderSelect) {
				patientId = ((CdsHooksRequest.OrderSelect) cdsHooksRequest).context.patientId;
				parameters = CdsHooksUtil.getParameters(
						((CdsHooksRequest.OrderSelect) cdsHooksRequest).context.draftOrders);
			} else if (cdsHooksRequest instanceof CdsHooksRequest.OrderSign) {
				patientId = ((CdsHooksRequest.OrderSign) cdsHooksRequest).context.patientId;
				parameters = CdsHooksUtil.getParameters(
						((CdsHooksRequest.OrderSign) cdsHooksRequest).context.draftOrders);
			} else {
				patientId = cdsHooksRequest.context.patientId;
			}

			List<String> expressions = CdsHooksUtil.getExpressions(servicePlan);
			BooleanType useServerData = null;
			Endpoint remoteDataEndpoint = null;
			if (cdsHooksRequest.fhirServer != null && !cdsHooksRequest.fhirServer.equals(baseUrl)) {
				useServerData = new BooleanType(false);
				remoteDataEndpoint = new Endpoint().setAddress(cdsHooksRequest.fhirServer);
				if (cdsHooksRequest.fhirAuthorization != null) {
					remoteDataEndpoint.addHeader(cdsHooksRequest.fhirAuthorization.tokenType + ": "
							+ cdsHooksRequest.fhirAuthorization.accessToken);
				}
			}
			Bundle data = CdsHooksUtil.getPrefetchResources(cdsHooksRequest);

			Parameters evaluationResult = cqlExecutor.getLibraryExecution(libraryExecution, logicId, patientId,
					expressions, parameters, useServerData, data, remoteDataEndpoint);

			List<Card.Link> links = resolvePlanLinks(servicePlan);
			List<Card> cards = new ArrayList<>();

			if (servicePlan.hasAction()) {
				resolveServicePlan(servicePlan.getAction(), evaluationResult, patientId, cards, links);
			}

			Cards result = new Cards();
			result.cards = cards;
			// Using GSON pretty print format as Jackson's is ugly
			String jsonResponse = new GsonBuilder().setPrettyPrinting().create().toJson(
					JsonParser.parseString(mapper.writeValueAsString(result)));
			logger.info(jsonResponse);
			response.getWriter().println(jsonResponse);
		} catch (BaseServerResponseException e) {
			ErrorHandling.handleError(response, "ERROR: Exception connecting to remote server.", e, myAppProperties);
			logger.error(e.toString());
		} catch (DataProviderException e) {
			ErrorHandling.handleError(response,"ERROR: Exception in DataProvider.", e, myAppProperties);
			logger.error(e.toString());
		} catch (CqlException e) {
			ErrorHandling.handleError(response,"ERROR: Exception in CQL Execution.", e, myAppProperties);
			logger.error(e.toString());
		} catch (Exception e) {
			logger.error(e.toString());
			throw new ServletException("ERROR: Exception in cds-hooks processing.", e);
		}
	}

	private void logRequestInfo(CdsHooksRequest request, String jsonRequest) {
		logger.info(jsonRequest);
		logger.info("cds-hooks hook instance: {}", request.hookInstance);
		logger.info("cds-hooks maxCodesPerQuery: {}", this.getProviderConfiguration().getMaxCodesPerQuery());
		logger.info("cds-hooks expandValueSets: {}", this.getProviderConfiguration().getExpandValueSets());
		logger.info("cds-hooks searchStyle: {}", this.getProviderConfiguration().getSearchStyle());
		logger.info("cds-hooks prefetch maxUriLength: {}", this.getProviderConfiguration().getMaxUriLength());
		logger.info("cds-hooks local server address: {}", myAppProperties.getServer_address());
		logger.info("cds-hooks fhir server address: {}", request.fhirServer);
		logger.info("cds-hooks cql_logging_enabled: {}", this.getProviderConfiguration().getCqlLoggingEnabled());
	}

	private List<Card.Link> resolvePlanLinks(PlanDefinition servicePlan) {
		List<Card.Link> links = new ArrayList<>();
		// links - listed on each card
		if (servicePlan.hasRelatedArtifact()) {
			servicePlan.getRelatedArtifact().forEach(
					ra -> {
						Card.Link link = new Card.Link();
						if (ra.hasDisplay()) link.setLabel(ra.getDisplay());
						if (ra.hasUrl()) link.setUrl(ra.getUrl());
						if (ra.hasExtension()) {
							link.setType(ra.getExtensionFirstRep().getValue().primitiveValue());
						}
						else link.setType("absolute"); // default
						links.add(link);
					}
			);
		}
		return links;
	}

	private void resolveServicePlan(List<PlanDefinition.PlanDefinitionActionComponent> actions,
									Parameters evaluationResults, String patientId, List<Card> cards,
									List<Card.Link> links) {
		Card card = new Card();
		if (links != null) card.setLinks(links);
		actions.forEach(
				action -> {
					if (resolveCondition(action, evaluationResults, patientId).get()) {
						if (action.hasTitle()) {
							card.setSummary(action.getTitle());
						}
						if (action.hasDescription()) {
							card.setDetail(action.getDescription());
						}
						if (action.hasDocumentation()) {
							card.setSource(resolveSource(action));
						}
						if (action.hasSelectionBehavior()) {
							card.setSelectionBehavior(action.getSelectionBehavior().toCode());
						}
						if (action.hasSelectionBehavior()) {
							card.setSelectionBehavior(action.getSelectionBehavior().toCode());
							Card.Suggestion suggestion = resolveSuggestions(action, patientId);
							card.setSuggestions(Collections.singletonList(suggestion));
						}
						if (action.hasDynamicValue()) {
							resolveDynamicActions(action, evaluationResults, patientId, card);
						}
						if (action.hasAction()) {
							resolveServicePlan(action.getAction(), evaluationResults, patientId, cards, links);
						}
						cards.add(card);
					}
				}
		);
	}

	public AtomicBoolean resolveCondition(PlanDefinition.PlanDefinitionActionComponent action,
										  Parameters evaluationResults, String patientId) {
		AtomicBoolean conditionMet = new AtomicBoolean(false);
		if (action.hasCondition()) {
			action.getCondition().forEach(
					condition -> {
						if (condition.hasExpression() && condition.getExpression().hasLanguage()
								&& condition.getExpression().hasExpression()) {
							Type conditionResult;
							if (condition.getExpression().getLanguage().equals("text/cql.identifier")) {
								conditionResult = evaluationResults.getParameter(
										condition.getExpression().getExpression());
							}
							else if (condition.getExpression().getLanguage().equals("text/cql")) {
								conditionResult = cqlExecutor.getExpressionExecution(cqlExecution,
												patientId, condition.getExpression().getExpression())
										.getParameter("return");
							}
							else conditionResult = new BooleanType(false);
							if (conditionResult != null) {
								conditionMet.set(conditionResult.isPrimitive()
										&& Boolean.parseBoolean(conditionResult.primitiveValue()));
							}
						}
					}
			);
		}
		return conditionMet;
	}

	public Card.Source resolveSource(PlanDefinition.PlanDefinitionActionComponent action) {
		Card.Source source = new Card.Source();
		RelatedArtifact documentation = action.getDocumentationFirstRep();
		if (documentation.hasDisplay()) {
			source.setLabel(documentation.getDisplay());
		}
		if (documentation.hasUrl()) {
			source.setUri(documentation.getUrl());
		}
		if (documentation.hasDocument() && documentation.getDocument().hasUrl()) {
			source.setIcon(documentation.getDocument().getUrl());
		}
		return source;
	}

	public Card.Suggestion resolveSuggestions(PlanDefinition.PlanDefinitionActionComponent action, String patientId) {
		Card.Suggestion suggestion = new Card.Suggestion();
		Card.Suggestion.Action suggAction = new Card.Suggestion.Action();
		suggAction.fhirContext = getFhirContext();
		if (action.hasPrefix()) suggestion.setLabel(action.getPrefix());
		boolean hasAction = false;
		if (action.hasDescription()) {
			suggAction.setDescription(action.getDescription());
			hasAction = true;
		}
		if (action.hasType() && action.getType().hasCoding()
				&& action.getType().getCodingFirstRep().hasCode()
				&& !action.getType().getCodingFirstRep().getCode().equals("fire-event")) {
			String actionCode = action.getType().getCodingFirstRep().getCode();
			suggAction.setType(actionCode);
			hasAction = true;
		}
		if (action.hasDefinitionCanonicalType() &&
				action.getDefinitionCanonicalType().getValue().contains("ActivityDefinition")) {
			suggAction.setType("create");
			IdType definitionId = new IdType(
					Canonicals.getResourceType(action.getDefinitionCanonicalType().getValue()),
					Canonicals.getIdPart(action.getDefinitionCanonicalType().getValue()));
			suggAction.setResource(applyEvaluator.apply(requestDetails, definitionId,
					patientId, null, null, null, null,
					null, null, null, null));
			hasAction = true;
		}
		if (hasAction) suggestion.setActions(Collections.singletonList(suggAction));
		return suggestion;
	}

	public void resolveDynamicActions(PlanDefinition.PlanDefinitionActionComponent action,
									  Parameters evaluationResults, String patientId, Card card) {
		action.getDynamicValue().forEach(
				dv -> {
					if (dv.hasPath() && dv.hasExpression() && dv.getExpression().hasLanguage()
							&& dv.getExpression().hasExpression()) {
						Object dynamicValueResult;
						if (dv.getExpression().getLanguage().equals("text/cql.identifier")) {
							dynamicValueResult = evaluationResults.getParameter(
									dv.getExpression().getExpression());
						}
						else {
							dynamicValueResult = cqlExecutor.getExpressionExecution(cqlExecution,
											patientId, dv.getExpression().getExpression())
									.getParameter("return");
						}
						if (dynamicValueResult != null) {
							if (dv.getPath().endsWith("title")) {
								card.setSummary(dynamicValueResult.toString());
							} else if (dv.getPath().endsWith("description")) {
								card.setDetail(dynamicValueResult.toString());
								if (card.getSuggestions() != null
										&& card.getSuggestions().get(0).getActions() != null) {
									card.getSuggestions().get(0).getActions().get(0).setDescription(
											dynamicValueResult.toString());
								}
							} else if (dv.getPath().endsWith("extension")) {
								card.setIndicator(dynamicValueResult.toString());
							} else if (card.getSuggestions() != null
									&& card.getSuggestions().get(0).getActions() != null
									&& card.getSuggestions().get((0)).getActions().get(0).getResource() != null) {
								modelResolver.setValue(
										card.getSuggestions().get((0)).getActions().get(0).getResource(),
										dv.getPath(), dynamicValueResult);
							}
						}
					}
				}
		);
	}

	private JsonObject getServices() {
		JsonObject services = new JsonObject();
		services.add("services", this.cdsServicesCache.getCdsServiceCache().get());
		return services;
	}

	public DebugMap getDebugMap() {
		DebugMap debugMap = new DebugMap();
		if (cqlProperties.getOptions().getCqlEngineOptions().isDebugLoggingEnabled()) {
			debugMap.setIsLoggingEnabled(true);
		}
		return debugMap;
	}

	@Override
	public DaoRegistry getDaoRegistry() {
		return this.daoRegistry;
	}
}
