package org.opencds.cqf.ruler.cr.r4.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RequestGroup;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.cr.r4.ExpressionEvaluation;
import org.opencds.cqf.ruler.cr.r4.builder.AttachmentBuilder;
import org.opencds.cqf.ruler.cr.r4.builder.CarePlanActivityBuilder;
import org.opencds.cqf.ruler.cr.r4.builder.CarePlanBuilder;
import org.opencds.cqf.ruler.cr.r4.builder.ExtensionBuilder;
import org.opencds.cqf.ruler.cr.r4.builder.ReferenceBuilder;
import org.opencds.cqf.ruler.cr.r4.builder.RelatedArtifactBuilder;
import org.opencds.cqf.ruler.cr.r4.builder.RequestGroupActionBuilder;
import org.opencds.cqf.ruler.cr.r4.builder.RequestGroupBuilder;
import org.opencds.cqf.ruler.cr.r4.helper.ContainedHelper;
import org.opencds.cqf.ruler.utility.CanonicalUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.cql.r4.helper.CanonicalHelper;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class PlanDefinitionApplyProvider implements OperationProvider, CanonicalUtilities {

	@Autowired
	private ExpressionEvaluation expressionEvaluation;
	@Autowired
	private ActivityDefinitionApplyProvider activityDefinitionApplyProvider;

	@Autowired
	private IFhirResourceDao<PlanDefinition> planDefinitionDao;
	@Autowired
	private IFhirResourceDao<ActivityDefinition> activityDefinitionDao;

	@Autowired
	private FhirContext fhirContext;

	private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionApplyProvider.class);

	public IFhirResourceDao<PlanDefinition> getDao() {
		return this.planDefinitionDao;
	}

	@Operation(name = "$apply", idempotent = true, type = PlanDefinition.class)
	public CarePlan applyPlanDefinition(RequestDetails theRequest, @IdParam IdType theId,
			@OperationParam(name = "patient") String patientId,
			@OperationParam(name = "encounter") String encounterId,
			@OperationParam(name = "practitioner") String practitionerId,
			@OperationParam(name = "organization") String organizationId,
			@OperationParam(name = "userType") String userType,
			@OperationParam(name = "userLanguage") String userLanguage,
			@OperationParam(name = "userTaskContext") String userTaskContext,
			@OperationParam(name = "setting") String setting,
			@OperationParam(name = "settingContext") String settingContext)
			throws IOException, FHIRException {
		PlanDefinition planDefinition = this.planDefinitionDao.read(theId);

		if (planDefinition == null) {
			throw new IllegalArgumentException("Couldn't find PlanDefinition " + theId);
		}

		logger.info("Performing $apply operation on PlanDefinition/" + theId);

		CarePlanBuilder builder = new CarePlanBuilder();

		builder.buildInstantiatesCanonical(planDefinition.getUrl())
				.buildSubject(new Reference(patientId)).buildStatus(CarePlan.CarePlanStatus.DRAFT);

		if (encounterId != null)
			builder.buildEncounter(new Reference(encounterId));
		if (practitionerId != null)
			builder.buildAuthor(new Reference(practitionerId));
		if (organizationId != null)
			builder.buildAuthor(new Reference(organizationId));
		if (userLanguage != null)
			builder.buildLanguage(userLanguage);

		// Each Group of actions shares a RequestGroup
		RequestGroupBuilder requestGroupBuilder = new RequestGroupBuilder().buildStatus().buildIntent();

		Session session = new Session(planDefinition, builder, patientId, encounterId, practitionerId, organizationId,
				userType, userLanguage, userTaskContext, setting, settingContext, requestGroupBuilder);

		return (CarePlan) ContainedHelper.liftContainedResourcesToParent(resolveActions(session, theRequest));
	}

	private CarePlan resolveActions(Session session, RequestDetails theRequest) {

		RequestGroup theRequestGroup = session.getRequestGroupBuilder().build();
		for (PlanDefinition.PlanDefinitionActionComponent action : session.getPlanDefinition().getAction()) {
			// TODO - Apply input/output dataRequirements?
			if (meetsConditions(session, action, theRequest)) {
				Resource result = resolveDefinition(session, action, theRequest);
				RequestGroupActionComponent currentActionTarget = null;
				if (result != null) {
					currentActionTarget = new RequestGroupActionBuilder()
							.buildResource(new Reference("#" + result.getId()))
							.build();
					session
							.getRequestGroupBuilder()
							.buildContained(result)
							.addAction(currentActionTarget);
				}
				resolveDynamicActions(session, action, currentActionTarget, theRequest);
			}
		}

		if (theRequestGroup.getId() == null) {
			theRequestGroup.setId(UUID.randomUUID().toString());
		}

		session.getCarePlanBuilder().buildContained(theRequestGroup).buildActivity(
				new CarePlanActivityBuilder().buildReference(new Reference("#" + theRequestGroup.getId())).build());

		return session.getCarePlan();
	}

	private Resource resolveDefinition(Session session, PlanDefinition.PlanDefinitionActionComponent action,
			RequestDetails theRequest) {
		Resource result = null;
		if (action.hasDefinition()) {
			logger.debug("Resolving definition " + action.getDefinitionCanonicalType().getValue());
			String definition = action.getDefinitionCanonicalType().getValue();
			if (definition.contains(session.getPlanDefinition().fhirType())) {
				IdType id = new IdType(definition);
				CarePlan plan;
				try {
					plan = applyPlanDefinition(theRequest,
							id,
							session.getPatientId(),
							session.getEncounterId(),
							session.getPractitionerId(),
							session.getOrganizationId(),
							session.getUserType(),
							session.getUserLanguage(),
							session.getUserTaskContext(),
							session.getSetting(),
							session.getSettingContext());

					if (plan.getId() == null) {
						plan.setId(UUID.randomUUID().toString());
					}

					for (CanonicalType c : plan.getInstantiatesCanonical()) {
						session.getCarePlanBuilder().buildInstantiatesCanonical(c.getValueAsString());
					}
					result = plan;

				} catch (IOException e) {
					e.printStackTrace();
					logger.error("nested plan failed");
				}
			}

			else {
				try {
					if (action.getDefinitionCanonicalType().getValue().startsWith("#")) {
						result = this.activityDefinitionApplyProvider.resolveActivityDefinition(
								(ActivityDefinition) resolveContained(session.getPlanDefinition(),
										action.getDefinitionCanonicalType().getValue()),
								session.getPatientId(), session.getPractitionerId(), session.getOrganizationId(), theRequest);
					} else {
						result = this.activityDefinitionApplyProvider.apply(
								theRequest, new IdType(CanonicalHelper.getId(action.getDefinitionCanonicalType())),
								session.getPatientId(), session.getEncounterId(), session.getPractitionerId(),
								session.getOrganizationId(), null, session.getUserLanguage(),
								session.getUserTaskContext(), session.getSetting(), session.getSettingContext());
					}

					if (result.getId() == null) {
						logger.warn("ActivityDefinition %s returned resource with no id, setting one",
								action.getDefinitionCanonicalType().getId());
						result.setId(UUID.randomUUID().toString());
					}

				} catch (Exception e) {
					logger.error("ERROR: ActivityDefinition %s could not be applied and threw exception %s",
							action.getDefinition(), e.toString());
				}
			}
		}
		return result;
	}

	private void resolveDynamicActions(Session session, PlanDefinition.PlanDefinitionActionComponent action,
			RequestGroupActionComponent currentActionTarget, RequestDetails theRequest) {
		for (PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue : action.getDynamicValue()) {
			logger.info(
					String.format("Resolving dynamic value %s %s", dynamicValue.getPath(), dynamicValue.getExpression()));
			Object result = null;
			if (dynamicValue.hasExpression()) {
				String language = dynamicValue.getExpression().getLanguage();
				if (language.equals("text/cql.identifier") || language.equals("text/cql-identifier")
						|| language.equals("text/cql.name") || language.equals("text/cql-name")) {
					result = expressionEvaluation.evaluateInContext(session.getPlanDefinition(),
							dynamicValue.getExpression().getExpression(), session.getPatientId(), true, theRequest);

				} else {

					result = expressionEvaluation.evaluateInContext(session.getPlanDefinition(),
							dynamicValue.getExpression().getExpression(), session.getPatientId(), theRequest);
				}

				if (dynamicValue.hasPath() && dynamicValue.getPath().equals("$this")) {
					session.setCarePlan((CarePlan) result);
				}

				else {

					// TODO - likely need more date transformations
					if (result instanceof DateTime) {
						result = Date.from(((DateTime) result).getDateTime().toInstant());
					}

					else if (result instanceof String) {
						result = new StringType((String) result);
					}

					if (currentActionTarget != null) {
						if (dynamicValue.getPath().contains("%action")) {
							try {
								currentActionTarget.setProperty(
										dynamicValue.getPath().substring(dynamicValue.getPath().lastIndexOf(".") + 1),
										(Base) result);
							} catch (Exception e) {
								throw new RuntimeException(
										String.format("Could not set path %s to value: %s", dynamicValue.getPath(), result));
							}
						}
					}
				}
			}
		}
	}

	private Boolean meetsConditions(Session session, PlanDefinition.PlanDefinitionActionComponent action,
			RequestDetails theRequest) {
		if (action.hasAction()) {
			for (PlanDefinition.PlanDefinitionActionComponent containedAction : action.getAction()) {
				meetsConditions(session, containedAction, theRequest);
			}
		}
		for (PlanDefinition.PlanDefinitionActionConditionComponent condition : action.getCondition()) {
			// TODO start
			// TODO stop
			if (condition.hasExpression() && condition.getExpression().hasDescription()) {
				logger.info("Resolving condition with description: " + condition.getExpression().getDescription());
			}
			if (condition.getKind() == PlanDefinition.ActionConditionKind.APPLICABILITY) {
				if (condition.hasExpression() && condition.getExpression().hasLanguage()
						&& !condition.getExpression().getLanguage().equals("text/cql")
						&& !condition.getExpression().getLanguage().equals("text/cql.identifier")
						&& !condition.getExpression().getLanguage().equals("text/cql-identifier")
						&& !condition.getExpression().getLanguage().equals("text/cql.name")
						&& !condition.getExpression().getLanguage().equals("text/cql-name")) {
					logger.warn(
							"An action language other than CQL was found: " + condition.getExpression().getLanguage());
					continue;
				}

				if (!condition.hasExpression()) {
					logger.error("Missing condition expression");
					throw new RuntimeException("Missing condition expression");
				}

				logger.info("Evaluating action condition expression " + condition.getExpression());
				String cql = condition.getExpression().getExpression();
				String language = condition.getExpression().getLanguage();
				Object result;
				if (language.equals("text/cql.identifier") || language.equals("text/cql-identifier")
						|| language.equals("text/cql.name") || language.equals("text/cql-name")) {
					result = expressionEvaluation.evaluateInContext(session.getPlanDefinition(), cql, session.getPatientId(),
							true, theRequest);
				} else {
					result = expressionEvaluation.evaluateInContext(session.getPlanDefinition(), cql, session.getPatientId(),
							theRequest);
				}

				if (result == null) {
					logger.warn("Expression Returned null");
					return false;
				}

				if (!(result instanceof Boolean)) {
					logger.warn("The condition returned a non-boolean value: " + result.getClass().getSimpleName());
					continue;
				}

				if (!(Boolean) result) {
					logger.info("The result of condition expression %s is false", condition.getExpression());
					return false;
				}
			}
		}
		return true;
	}

	// For library use
	public CarePlan resolveCdsHooksPlanDefinition(Context context, PlanDefinition planDefinition, String patientId,
			RequestDetails theRequest) {

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
				theRequest);

		CarePlanActivityBuilder carePlanActivityBuilder = new CarePlanActivityBuilder();
		carePlanActivityBuilder.buildReferenceTarget(requestGroupBuilder.build());
		carePlanBuilder.buildActivity(carePlanActivityBuilder.build());

		return carePlanBuilder.build();
	}

	private void resolveActions(List<PlanDefinition.PlanDefinitionActionComponent> actions, Context context,
			String patientId, RequestGroupBuilder requestGroupBuilder,
			List<RequestGroup.RequestGroupActionComponent> actionComponents, RequestDetails theRequest) {
		for (PlanDefinition.PlanDefinitionActionComponent action : actions) {
			boolean conditionsMet = true;
			for (PlanDefinition.PlanDefinitionActionConditionComponent condition : action.getCondition()) {
				if (condition.getKind() == PlanDefinition.ActionConditionKind.APPLICABILITY) {
					if (!condition.hasExpression()) {
						continue;
					}

					if (condition.hasExpression() && !condition.getExpression().hasExpression()) {
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
					if (action.hasDefinition()) {
						if (action.getDefinitionCanonicalType().getValue().contains("ActivityDefinition")) {
							ActivityDefinition activityDefinition = this.activityDefinitionDao.read(
									new IdType("ActivityDefinition", action.getDefinitionCanonicalType().getId()));
							if (activityDefinition.hasDescription()) {
								actionBuilder.buildDescripition(activityDefinition.getDescription());
							}
							try {
								this.activityDefinitionApplyProvider
										.apply(theRequest, new IdType(action.getDefinitionCanonicalType().getId()), patientId,
												null,
												null, null, null, null, null, null, null)
										.setId(UUID.randomUUID().toString());
							} catch (FHIRException | ClassNotFoundException | InstantiationException
									| IllegalAccessException e) {
								throw new RuntimeException("Error applying ActivityDefinition " + e.getMessage());
							}

							Parameters inParams = new Parameters();
							inParams.addParameter().setName("patient").setValue(new StringType(patientId));
							Parameters outParams = this.fhirContext
									.newRestfulGenericClient(theRequest.getFhirServerBase()).operation()
									.onInstance(new IdDt("ActivityDefinition", action.getDefinition().getId()))
									.named("$apply").withParameters(inParams).useHttpGet().execute();

							List<Parameters.ParametersParameterComponent> response = outParams.getParameter();
							Resource resource = response.get(0).getResource().setId(UUID.randomUUID().toString());
							actionBuilder.buildResourceTarget(resource);
							actionBuilder
									.buildResource(new ReferenceBuilder().buildReference(resource.getId()).build());
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
								}
							}
						}
					}

					if (!actionBuilder.build().isEmpty()) {
						actionComponents.add(actionBuilder.build());
					}

					if (action.hasAction()) {
						resolveActions(action.getAction(), context, patientId, requestGroupBuilder, actionComponents,
								theRequest);
					}
				}
			}
		}
		requestGroupBuilder.buildAction(new ArrayList<>(actionComponents));
	}

	public Resource resolveContained(DomainResource resource, String id) {
		for (Resource res : resource.getContained()) {
			if (res.hasIdElement()) {
				if (res.getIdElement().getIdPart().equals(id)) {
					return res;
				}
			}
		}

		throw new RuntimeException(
				String.format("Resource %s does not contain resource with id %s", resource.fhirType(), id));
	}
}

class Session {
	private final String patientId;
	private final PlanDefinition planDefinition;
	private final String practitionerId;
	private final String organizationId;
	private final String userType;
	private final String userLanguage;
	private final String userTaskContext;
	private final String setting;
	private final String settingContext;
	private CarePlanBuilder carePlanBuilder;
	private final String encounterId;
	private final RequestGroupBuilder requestGroupBuilder;

	public Session(PlanDefinition planDefinition, CarePlanBuilder builder, String patientId, String encounterId,
			String practitionerId, String organizationId, String userType, String userLanguage, String userTaskContext,
			String setting, String settingContext, RequestGroupBuilder requestGroupBuilder) {
		this.patientId = patientId;
		this.planDefinition = planDefinition;
		this.carePlanBuilder = builder;
		this.encounterId = encounterId;
		this.practitionerId = practitionerId;
		this.organizationId = organizationId;
		this.userType = userType;
		this.userLanguage = userLanguage;
		this.userTaskContext = userTaskContext;
		this.setting = setting;
		this.settingContext = settingContext;
		this.requestGroupBuilder = requestGroupBuilder;
	}

	public PlanDefinition getPlanDefinition() {
		return this.planDefinition;
	}

	public String getPatientId() {
		return patientId;
	}

	public CarePlan getCarePlan() {
		return carePlanBuilder.build();
	}

	public void setCarePlan(CarePlan carePlan) {
		this.carePlanBuilder = new CarePlanBuilder(carePlan);
	}

	public String getEncounterId() {
		return this.encounterId;
	}

	public String getPractitionerId() {
		return practitionerId;
	}

	public String getOrganizationId() {
		return organizationId;
	}

	public String getUserType() {
		return userType;
	}

	public String getUserLanguage() {
		return userLanguage;
	}

	public String getUserTaskContext() {
		return userTaskContext;
	}

	public String getSetting() {
		return setting;
	}

	public String getSettingContext() {
		return settingContext;
	}

	public CarePlanBuilder getCarePlanBuilder() {
		return carePlanBuilder;
	}

	public RequestGroupBuilder getRequestGroupBuilder() {
		return requestGroupBuilder;
	}
}
