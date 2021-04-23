package org.opencds.cqf.ruler.r4.providers;

import java.io.IOException;
import java.util.*;

import javax.inject.Inject;
import javax.jms.IllegalStateRuntimeException;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.ruler.common.exceptions.NotImplementedException;
import org.opencds.cqf.ruler.common.config.HapiProperties;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.ruler.r4.builders.AttachmentBuilder;
import org.opencds.cqf.ruler.r4.builders.CarePlanActivityBuilder;
import org.opencds.cqf.ruler.r4.builders.CarePlanBuilder;
import org.opencds.cqf.ruler.r4.builders.ExtensionBuilder;
import org.opencds.cqf.ruler.r4.builders.JavaDateBuilder;
import org.opencds.cqf.ruler.r4.builders.ReferenceBuilder;
import org.opencds.cqf.ruler.r4.builders.RelatedArtifactBuilder;
import org.opencds.cqf.ruler.r4.builders.RequestGroupActionBuilder;
import org.opencds.cqf.ruler.r4.builders.RequestGroupBuilder;
import org.opencds.cqf.ruler.r4.dal.RulerDal;
import org.opencds.cqf.ruler.r4.helpers.CanonicalHelper;
import org.opencds.cqf.ruler.r4.helpers.ContainedHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

@Component
public class PlanDefinitionApplyProvider {

  private ModelResolver modelResolver;
  private ActivityDefinitionProcessor activityDefinitionProcessor;
  private RulerDal fhirDal;
  private LibraryProcessor libraryProcessor;
  private FhirContext fhirContext;
  private IFhirResourceDao<PlanDefinition> planDefinitionDao;

  private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionApplyProvider.class);

  @Inject
  public PlanDefinitionApplyProvider(
      FhirContext fhirContext,
      ActivityDefinitionProcessor activityDefinitionProcessor,
      RulerDal rulerDal,
      LibraryProcessor libraryProcessor) {
    ModelResolverFactory modelResolverFactory = new FhirModelResolverFactory();
    this.modelResolver = modelResolverFactory.create(fhirContext.getVersion().getVersion().getFhirVersionString());
    this.libraryProcessor = libraryProcessor;
    this.activityDefinitionProcessor = activityDefinitionProcessor;
    this.fhirDal = rulerDal;
    this.fhirContext = fhirContext;
  }

  public IFhirResourceDao<PlanDefinition> getDao() {
    return this.planDefinitionDao;
  }

  @Operation(name = "$apply", idempotent = true, type = PlanDefinition.class)
  public CarePlan applyPlanDefinition(
      @IdParam IdType theId,
      @OperationParam(name = "subject") String patientId,
      @OperationParam(name = "encounter") String encounterId,
      @OperationParam(name = "practitioner") String practitionerId,
      @OperationParam(name = "organization") String organizationId,
      @OperationParam(name = "userType") String userType,
      @OperationParam(name = "userLanguage") String userLanguage,
      @OperationParam(name = "userTaskContext") String userTaskContext,
      @OperationParam(name = "setting") String setting,
      @OperationParam(name = "settingContext") String settingContext,
      @OperationParam(name = "mergeNestedCarePlans") boolean mergeNestedCarePlans,
      @OperationParam(name = "parameters") IBaseParameters parameters,
      @OperationParam(name = "useServerData") boolean useServerData,
      @OperationParam(name = "data") IBaseBundle bundle,
      @OperationParam(name = "prefetchData") DataRequirement prefetchData,
      @OperationParam(name = "prefetchData.key") String prefetchDataKey,
      @OperationParam(name = "prefetchData.description") DataRequirement prefetchDataDescription,
      @OperationParam(name = "prefetchData.data") IBaseBundle prefetchDataData,
      @OperationParam(name = "dataEndpoint") IBaseResource dataEndpoint,
      @OperationParam(name = "contentEndpoint") IBaseResource contentEndpoint,
      @OperationParam(name = "terminologyEndpoint") IBaseResource terminologyEndpoint)
      throws IOException, FHIRException {

    IBaseResource basePlanDefinition = this.fhirDal.read(theId);
    PlanDefinition planDefinition;

    if (basePlanDefinition == null) {
      throw new IllegalArgumentException("Couldn't find PlanDefinition " + theId);
    }
    if (!(basePlanDefinition instanceof PlanDefinition)) {
      throw new IllegalArgumentException(
          "The planDefinition passed to FhirDal was "
              + "not a valid instance of PlanDefinition.class");
    }

    if (!(dataEndpoint instanceof Endpoint)) {
      throw new IllegalArgumentException(
          "dataEndpoint was not a proper instance of Endpoint.class");
    }

    if (!(contentEndpoint instanceof Endpoint)) {
      throw new IllegalArgumentException(
          "dataEndpoint was not a proper instance of Endpoint.class");
    }

    if (!(terminologyEndpoint instanceof Endpoint)) {
      throw new IllegalArgumentException(
          "dataEndpoint was not a proper instance of Endpoint.class");
    }

    planDefinition = (PlanDefinition) basePlanDefinition;

    logger.info("Performing $apply operation on PlanDefinition/" + theId);

    CarePlanBuilder builder = new CarePlanBuilder();

    builder
        .buildInstantiatesCanonical(planDefinition.getIdElement().getIdPart())
        .buildSubject(new Reference(patientId))
        .buildStatus(CarePlan.CarePlanStatus.DRAFT);

    if (encounterId != null) builder.buildEncounter(new Reference(encounterId));
    if (practitionerId != null) builder.buildAuthor(new Reference(practitionerId));
    if (organizationId != null) builder.buildAuthor(new Reference(organizationId));
    if (userLanguage != null) builder.buildLanguage(userLanguage);

    // Each Group of actions shares a RequestGroup
    RequestGroupBuilder requestGroupBuilder = new RequestGroupBuilder().buildStatus().buildIntent();

    Session session =
        new Session(
            planDefinition,
            builder,
            patientId,
            encounterId,
            practitionerId,
            organizationId,
            userType,
            userLanguage,
            userTaskContext,
            setting,
            settingContext,
            requestGroupBuilder,
            parameters,
            contentEndpoint,
            terminologyEndpoint,
            dataEndpoint,
            bundle,
            useServerData,
            prefetchData,
            mergeNestedCarePlans,
            prefetchDataData,
            prefetchDataDescription,
            prefetchDataKey);

    return (CarePlan) ContainedHelper.liftContainedResourcesToParent(resolveActions(session));
  }

  private CarePlan resolveActions(Session session) {
    for (PlanDefinition.PlanDefinitionActionComponent action : session.planDefinition.getAction()) {
      // TODO - Apply input/output dataRequirements?
      if (meetsConditions(session, action)) {
        resolveDefinition(session, action);
        resolveDynamicActions(session, action);
      }
    }

    RequestGroup result = session.requestGroupBuilder.build();

    if (result.getId() == null) {
      result.setId(UUID.randomUUID().toString());
    }

    session
        .carePlanBuilder
        .buildContained(result)
        .buildActivity(
            new CarePlanActivityBuilder()
                .buildReference(new Reference("#" + result.getId()))
                .build());

    return session.carePlanBuilder.build();
  }

  private void resolveDefinition(Session session, PlanDefinition.PlanDefinitionActionComponent action) {
    if (action.hasDefinitionCanonicalType()) {
      logger.debug("Resolving definition " + action.getDefinitionCanonicalType().getValue());
      CanonicalType definition = action.getDefinitionCanonicalType();
      switch (CanonicalHelper.getResourceName(definition)) {
        case ("PlanDefinition"): applyNestedPlanDefinition(session, new IdType(definition)); break;
        case ("ActivityDefinition"): applyActivityDefinition(session, definition); break;
        case ("Questionnaire"): throw new NotImplementedException("Questionnaire definition evaluation is not yet implemented.");
        default: throw new RuntimeException(String.format("Unknown action definition: ", definition));
      }
    } else if (action.hasDefinitionUriType()) {
      throw new NotImplementedException("Uri definition evaluation is not yet implemented");
    }
  }

  private void applyActivityDefinition(Session session, CanonicalType definition) {
    IBaseResource result;
    try {
      boolean referenceToContained = definition.getValue().startsWith("#");
      if (referenceToContained) {
        ActivityDefinition activityDefinition = (ActivityDefinition)resolveContained(session.planDefinition, definition.getValue());
        result =
            this.activityDefinitionProcessor.resolveActivityDefinition(
                activityDefinition,
                session.patientId,
                session.practitionerId,
                session.organizationId,
                session.parameters,
                session.contentEndpoint,
                session.terminologyEndpoint,
                session.dataEndpoint
                );
      } else {
        result =
        this.activityDefinitionProcessor.apply(
            new IdType(CanonicalHelper.getId(definition)),
            session.patientId,
            session.encounterId,
            session.practitionerId,
            session.organizationId,
            session.userType,
            session.userLanguage,
            session.userTaskContext,
            session.setting,
            session.settingContext,
            session.parameters,
            session.contentEndpoint,
            session.terminologyEndpoint,
            session.dataEndpoint
            );
      }

      if (result.getIdElement() == null) {
        logger.warn(
            "ActivityDefinition %s returned resource with no id, setting one",
            definition.getId());
        result.setId(new IdType(UUID.randomUUID().toString()));
      }

      session
          .requestGroupBuilder
          .buildContained((Resource)result)
          .addAction(
              new RequestGroupActionBuilder()
                  .buildResource(new Reference("#" + result.getIdElement().getIdPart()))
                  .build());

    } catch (Exception e) {
      logger.error(
          "ERROR: ActivityDefinition %s could not be applied and threw exception %s",
          definition, e.toString());
    }
  }

  private void applyNestedPlanDefinition(Session session, IdType id) {
    CarePlan plan;
    try {
      plan =
          applyPlanDefinition(
              id,
              session.patientId,
              session.encounterId,
              session.practitionerId,
              session.organizationId,
              session.userType,
              session.userLanguage,
              session.userTaskContext,
              session.setting,
              session.settingContext,
              session.mergeNestedCarePlans,
              session.parameters,
              session.useServerData,
              session.bundle,
              session.prefetchData,
              session.prefetchDataKey,
              session.prefetchDataDescription,
              session.prefetchDataData,
              session.dataEndpoint,
              session.contentEndpoint,
              session.terminologyEndpoint);

      if (plan.getId() == null) {
        plan.setId(UUID.randomUUID().toString());
      }

      // Add an action to the request group which points to this CarePlan
      session
          .requestGroupBuilder
          .buildContained(plan)
          .addAction(
              new RequestGroupActionBuilder()
                  .buildResource(new Reference("#" + plan.getId()))
                  .build());

      for (CanonicalType c : plan.getInstantiatesCanonical()) {
        session.carePlanBuilder.buildInstantiatesCanonical(c.getValueAsString());
      }

    } catch (IOException e) {
      e.printStackTrace();
      logger.error("nested plan failed");
    }
  }

  private void resolveDynamicActions(
      Session session, PlanDefinition.PlanDefinitionActionComponent action) {
    for (PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue :
        action.getDynamicValue()) {
      logger.info(
          "Resolving dynamic value %s %s", dynamicValue.getPath(), dynamicValue.getExpression());

      ensureDynamicValueExpression(dynamicValue);
      if (dynamicValue.getExpression().hasLanguage()) {

        ensurePrimaryLibrary(session);
        Object result = evaluateConditionOrDynamicValue(dynamicValue, session);
        // TODO: Rename bundle
        if (dynamicValue.hasPath() && dynamicValue.getPath().equals("$this")) {
          session.carePlanBuilder = new CarePlanBuilder((CarePlan) result);
        } else {

          // TODO - likely need more date transformations
          if (result instanceof DateTime) {
            result = new JavaDateBuilder().buildFromDateTime((DateTime) result).build();
          }

          // TODO: This totally might not work
          else if (result instanceof String) {
            result = new StringType((String) result);
          }

          this.modelResolver.setValue(session.carePlanBuilder, dynamicValue.getPath(), result);
        }
      }
    }
  }

  private Boolean meetsConditions(Session session, PlanDefinition.PlanDefinitionActionComponent action) {
    if (action.hasAction()) {
      for (PlanDefinition.PlanDefinitionActionComponent containedAction : action.getAction()) {
        meetsConditions(session, containedAction);
      }
    }
    for (PlanDefinition.PlanDefinitionActionConditionComponent condition : action.getCondition()) {
      ensureConditionExpression(condition);
      if (condition.getExpression().hasLanguage()) {

        ensurePrimaryLibrary(session);
        Object result = evaluateConditionOrDynamicValue(condition, session);

        if (result == null) {
          logger.warn("Expression Returned null");
          return false;
        }

        if (!(result instanceof Boolean)) {
          logger.warn(
              "The condition returned a non-boolean value: " + result.getClass().getSimpleName());
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

  public CarePlan resolveCdsHooksPlanDefinition(
      PlanDefinition planDefinition,
      String patientId,
      String encounterId,
      String practitionerId,
      String organizationId,
      String userType,
      String userLanguage,
      String userTaskContext,
      String setting,
      String settingContext,
      boolean mergeNestedCarePlans,
      IBaseParameters parameters,
      boolean useServerData,
      IBaseBundle bundle,
      DataRequirement prefetchData,
      String prefetchDataKey,
      DataRequirement prefetchDataDescription,
      IBaseBundle prefetchDataData,
      IBaseResource dataEndpoint,
      IBaseResource contentEndpoint,
      IBaseResource terminologyEndpoint) {

    CarePlanBuilder carePlanBuilder = new CarePlanBuilder();
    RequestGroupBuilder requestGroupBuilder = new RequestGroupBuilder().buildStatus().buildIntent();
    Session session =
        new Session(
            planDefinition,
            carePlanBuilder,
            patientId,
            encounterId,
            practitionerId,
            organizationId,
            userType,
            userLanguage,
            userTaskContext,
            setting,
            settingContext,
            requestGroupBuilder,
            parameters,
            contentEndpoint,
            terminologyEndpoint,
            dataEndpoint,
            bundle,
            useServerData,
            prefetchData,
            mergeNestedCarePlans,
            prefetchDataData,
            prefetchDataDescription,
            prefetchDataKey);

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

    resolveCdsHooksActions(session);

    CarePlanActivityBuilder carePlanActivityBuilder = new CarePlanActivityBuilder();
    carePlanActivityBuilder.buildReferenceTarget(requestGroupBuilder.build());
    carePlanBuilder.buildActivity(carePlanActivityBuilder.build());

    return carePlanBuilder.build();
  }

  private void resolveCdsHooksActions(Session session) {
    ArrayList<RequestGroup.RequestGroupActionComponent> actionConditionComponents =
        new ArrayList<>();

    for (PlanDefinition.PlanDefinitionActionComponent action : session.planDefinition.getAction()) {
      boolean conditionsMet = true;
      for (PlanDefinition.PlanDefinitionActionConditionComponent condition :
          action.getCondition()) {
        ensureConditionExpression(condition);
        if (condition.getExpression().hasLanguage()) {

          ensurePrimaryLibrary(session);
          Object result = evaluateConditionOrDynamicValue(condition, session);

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
              IdType idType =
                  new IdType("ActivityDefinition", action.getDefinitionCanonicalType().getId());
              IBaseResource toCheck = this.fhirDal.read(idType);
              ActivityDefinition activityDefinition;

              if (toCheck == null) {
                throw new IllegalArgumentException(
                    "Couldn't find ActivityDefinition " + idType.getId());
              }
              if (!(toCheck instanceof ActivityDefinition)) {
                throw new IllegalArgumentException(
                    "The ActivityDefinition passed to rulerDal was "
                        + "not a proper instance f ActivityDefinition.class");
              }
              activityDefinition = (ActivityDefinition) toCheck;

              if (activityDefinition.hasDescription()) {
                actionBuilder.buildDescripition(activityDefinition.getDescription());
              }
              try {
                this.activityDefinitionProcessor
                    .apply(
                        new IdType(action.getDefinitionCanonicalType().getId()),
                        session.patientId,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null)
                    .setId(UUID.randomUUID().toString());
              } catch (FHIRException
                  | ClassNotFoundException
                  | InstantiationException
                  | IllegalAccessException e) {
                throw new RuntimeException("Error applying ActivityDefinition " + e.getMessage());
              }

              Parameters inParams = new Parameters();
              inParams
                  .addParameter()
                  .setName("patient")
                  .setValue(new StringType(session.patientId));
              Parameters outParams =
                  this.fhirContext
                      .newRestfulGenericClient(HapiProperties.getServerBase())
                      .operation()
                      .onInstance(new IdDt("ActivityDefinition", action.getDefinition().getId()))
                      .named("$apply")
                      .withParameters(inParams)
                      .useHttpGet()
                      .execute();

              List<Parameters.ParametersParameterComponent> response = outParams.getParameter();
              Resource resource = response.get(0).getResource().setId(UUID.randomUUID().toString());
              actionBuilder.buildResourceTarget(resource);
              actionBuilder.buildResource(
                  new ReferenceBuilder().buildReference(resource.getId()).build());
            }
          }

          // Dynamic values populate the RequestGroup - there is a bit of hijacking going
          // on here...
          if (action.hasDynamicValue()) {
            for (PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue :
                action.getDynamicValue()) {
              ensureConditionExpression(condition);
              if (condition.getExpression().hasLanguage()) {

                ensurePrimaryLibrary(session);
                if (dynamicValue.hasPath() && dynamicValue.hasExpression()) {
                  if (dynamicValue.getPath().endsWith("title")) { // summary
                    actionBuilder.buildTitle(
                        (String) ensureStringResult(evaluateConditionOrDynamicValue(condition, session)));
                  } else if (dynamicValue.getPath().endsWith("description")) { // detail
                    actionBuilder.buildDescripition(
                        (String) ensureStringResult(evaluateConditionOrDynamicValue(condition, session)));
                  } else if (dynamicValue.getPath().endsWith("extension")) { // indicator
                    actionBuilder.buildExtension(
                        (String) ensureStringResult(evaluateConditionOrDynamicValue(condition, session)));
                  }
                }
              }
            }

            if (!actionBuilder.build().isEmpty()) {
              actionConditionComponents.add(actionBuilder.build());
            }

            if (action.hasAction()) {
              resolveCdsHooksActions(session);
            }
          }
        }
      }
    }
    session.requestGroupBuilder.buildAction(actionConditionComponents);
  }

  private void ensurePrimaryLibrary(Session session) {
    if (session.planDefinition.getLibrary().size() != 1) {
      throw new IllegalArgumentException(
          "Session's PlanDefinition's Library must only "
              + "include the primary liibrary. ");
    }
  }

  private void ensureConditionExpression(PlanDefinition.PlanDefinitionActionConditionComponent condition) {
    if (!condition.hasExpression()) {
      logger.error("Missing condition expression");
      throw new RuntimeException("Missing condition expression");
    }
  }

  private void ensureDynamicValueExpression(PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue) {
    if (!dynamicValue.hasExpression()) {
      logger.error("Missing condition expression");
      throw new RuntimeException("Missing condition expression");
    }
  }

  private Object ensureStringResult(Object result) {
    if (!(result instanceof String))
      throw new IllegalStateRuntimeException("Result not instance of String");
    return (String) result;
  }

  private Object evaluateConditionOrDynamicValue(BackboneElement backboneElement, Session session) {
    Object result = null;
    Set<String> expressions = new HashSet<>();
    String language = null;

    if (backboneElement instanceof PlanDefinition.PlanDefinitionActionConditionComponent) {
      language =
          ((PlanDefinition.PlanDefinitionActionConditionComponent) backboneElement)
              .getExpression()
              .getLanguage();
      expressions.add(
          ((PlanDefinition.PlanDefinitionActionConditionComponent) backboneElement)
              .getExpression()
              .getExpression());
    } else if (backboneElement
        instanceof PlanDefinition.PlanDefinitionActionDynamicValueComponent) {
      language =
          ((PlanDefinition.PlanDefinitionActionDynamicValueComponent) backboneElement)
              .getExpression()
              .getLanguage();
      expressions.add(
          ((PlanDefinition.PlanDefinitionActionDynamicValueComponent) backboneElement)
              .getExpression()
              .getExpression());
    } else {
      throw new IllegalStateRuntimeException(
          "Could not resolve backboneElement,"
              + " was not instance of DynamicValueComponent or ActionConditionComponent.");
    }

    // Assumption that this will evolve to contain many cases
    switch (language) {
      //                    case "text/cql":
      //                        expressionEvaluator.evaluate(new Task(),
      // dynamicValue.getExpression());
    case "text/cql-identifier":
      result =
          libraryProcessor.evaluate(
              session.planDefinition.getLibrary().get(0).toString(),
              session.patientId,
              session.parameters,
              session.contentEndpoint,
              session.terminologyEndpoint,
              session.dataEndpoint,
              session.bundle,
              expressions); break;
    case "text/cql.identifier":
    result =
        libraryProcessor.evaluate(
            session.planDefinition.getLibrary().get(0).toString(),
            session.patientId,
            session.parameters,
            session.contentEndpoint,
            session.terminologyEndpoint,
            session.dataEndpoint,
            session.bundle,
            expressions); break;
    case "text/cql.name":
      result =
          libraryProcessor.evaluate(
              session.planDefinition.getLibrary().get(0).toString(),
              session.patientId,
              session.parameters,
              session.contentEndpoint,
              session.terminologyEndpoint,
              session.dataEndpoint,
              session.bundle,
              expressions); break;
    default:
      logger.warn(
          "An action language other than CQL was found: "
              + language);
  }
    return result;
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
  public final String patientId;
  public final PlanDefinition planDefinition;
  public final String practitionerId;
  public final String organizationId;
  public final String userType;
  public final String userLanguage;
  public final String userTaskContext;
  public final String setting;
  public final String settingContext;
  public final String prefetchDataKey;
  public CarePlanBuilder carePlanBuilder;
  public final String encounterId;
  public final RequestGroupBuilder requestGroupBuilder;
  public IBaseParameters parameters;
  public IBaseResource contentEndpoint, terminologyEndpoint, dataEndpoint;
  public IBaseBundle bundle, prefetchDataData;
  public DataRequirement prefetchData, prefetchDataDescription;
  public boolean useServerData, mergeNestedCarePlans;

  public Session(
      PlanDefinition planDefinition,
      CarePlanBuilder builder,
      String patientId,
      String encounterId,
      String practitionerId,
      String organizationId,
      String userType,
      String userLanguage,
      String userTaskContext,
      String setting,
      String settingContext,
      RequestGroupBuilder requestGroupBuilder,
      IBaseParameters parameters,
      IBaseResource contentEndpoint,
      IBaseResource terminologyEndpoint,
      IBaseResource dataEndpoint,
      IBaseBundle bundle,
      boolean useServerData,
      DataRequirement prefetchData,
      boolean mergeNestedCarePlans,
      IBaseBundle prefetchDataData,
      DataRequirement prefetchDataDescription,
      String prefetchDataKey) {

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
    this.parameters = parameters;
    this.contentEndpoint = contentEndpoint;
    this.terminologyEndpoint = terminologyEndpoint;
    this.dataEndpoint = dataEndpoint;
    this.bundle = bundle;
    this.useServerData = useServerData;
    this.mergeNestedCarePlans = mergeNestedCarePlans;
    this.prefetchDataData = prefetchDataData;
    this.prefetchDataDescription = prefetchDataDescription;
    this.prefetchData = prefetchData;
    this.prefetchDataKey = prefetchDataKey;
  }
}
