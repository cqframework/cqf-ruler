package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.CodeSystemResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.ValueSetResourceProvider;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.*;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.builders.*;
import org.opencds.cqf.cdshooks.providers.DiscoveryDataProvider;
import org.opencds.cqf.cdshooks.providers.DiscoveryDataProviderDstu2;
import org.opencds.cqf.cdshooks.providers.DiscoveryDataProviderStu3;
import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.runtime.Tuple;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.exceptions.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class FHIRPlanDefinitionResourceProvider extends JpaResourceProviderDstu3<PlanDefinition> {

    private JpaDataProvider provider;
    private CqlExecutionProvider executionProvider;
    private STU3LibraryLoader libraryLoader;

    private static final Logger logger = LoggerFactory.getLogger(FHIRPlanDefinitionResourceProvider.class);

    public FHIRPlanDefinitionResourceProvider(Collection<IResourceProvider> providers) {
        this.provider = new JpaDataProvider(providers);

        JpaResourceProviderDstu3<ValueSet>   vs = (ValueSetResourceProvider)   provider.resolveResourceProvider("ValueSet");
        JpaResourceProviderDstu3<CodeSystem> cs = (CodeSystemResourceProvider) provider.resolveResourceProvider("CodeSystem");
        TerminologyProvider terminologyProvider = new JpaTerminologyProvider(vs, cs);
        this.provider.setTerminologyProvider(terminologyProvider);
        this.provider.setExpandValueSets(true);

        this.libraryLoader =
                new STU3LibraryLoader(
                        (LibraryResourceProvider) provider.resolveResourceProvider("Library"),
                        new LibraryManager(new ModelManager()), new ModelManager()
                );

        this.executionProvider = new CqlExecutionProvider(providers);
    }

    @Operation(name = "$apply", idempotent = true)
    public CarePlan applyPlanDefinition(
            @IdParam IdType theId,
            @RequiredParam(name="patient") String patientId,
            @OptionalParam(name="encounter") String encounterId,
            @OptionalParam(name="practitioner") String practitionerId,
            @OptionalParam(name="organization") String organizationId,
            @OptionalParam(name="userType") String userType,
            @OptionalParam(name="userLanguage") String userLanguage,
            @OptionalParam(name="userTaskContext") String userTaskContext,
            @OptionalParam(name="setting") String setting,
            @OptionalParam(name="settingContext") String settingContext)
            throws IOException, JAXBException, FHIRException
    {
        PlanDefinition planDefinition = this.getDao().read(theId);

        if (planDefinition == null) {
            throw new IllegalArgumentException("Couldn't find PlanDefintion " + theId);
        }

        logger.info("Performing $apply operation on PlanDefinition/" + theId);

        CarePlanBuilder builder = new CarePlanBuilder();

        builder
                .buildDefinition(new Reference(planDefinition.getIdElement().getIdPart()))
                .buildSubject(new Reference(patientId))
                .buildStatus(CarePlan.CarePlanStatus.DRAFT);

        if (encounterId != null) builder.buildContext(new Reference(encounterId));
        if (practitionerId != null) builder.buildAuthor(new Reference(practitionerId));
        if (organizationId != null) builder.buildAuthor(new Reference(organizationId));
        if (userLanguage != null) builder.buildLanguage(userLanguage);

        Session session =
                new Session(planDefinition, builder, patientId, encounterId, practitionerId,
                        organizationId, userType, userLanguage, userTaskContext, setting, settingContext);

        return resolveActions(session);
    }

    private CarePlan resolveActions(Session session) {
        for (PlanDefinition.PlanDefinitionActionComponent action : session.getPlanDefinition().getAction()) {
            // TODO - Apply input/output dataRequirements?
            if (meetsConditions(session, action)) {
                resolveDefinition(session, action);
                resolveDynamicActions(session, action);
            }
        }

        return session.getCarePlan();
    }

    private void resolveDefinition(Session session, PlanDefinition.PlanDefinitionActionComponent action) {
        if (action.hasDefinition()) {
            logger.debug("Resolving definition "+ action.getDefinition().getReference());
            Reference definition = action.getDefinition();
            if (definition.getReference().startsWith(session.getPlanDefinition().fhirType())) {
                logger.error("Currently cannot resolve nested PlanDefinitions");
                throw new NotImplementedException("Plan Definition refers to sub Plan Definition, this is not yet supported");
            }

            else {
                FHIRActivityDefinitionResourceProvider activitydefinitionProvider = new FHIRActivityDefinitionResourceProvider(provider.getCollectionProviders());
                Resource result;
                try {
                    if (action.getDefinition().getReferenceElement().getIdPart().startsWith("#")) {
                        result = activitydefinitionProvider.resolveActivityDefinition(
                                (ActivityDefinition) resolveContained(session.getPlanDefinition(),
                                        action.getDefinition().getReferenceElement().getIdPart()),
                                session.getPatientId(), session.getPractionerId(), session.getOrganizationId()
                        );
                    }
                    else {
                        result = activitydefinitionProvider.apply(
                                new IdType(action.getDefinition().getReferenceElement().getIdPart()),
                                session.getPatientId(),
                                session.getEncounterId(),
                                session.getPractionerId(),
                                session.getOrganizationId(),
                                null,
                                session.getUserLanguage(),
                                session.getUserTaskContext(),
                                session.getSetting(),
                                session.getSettingContext()
                        );
                    }

                    if (result.getId() == null) {
                        logger.warn("ActivityDefintion %s returned resource with no id, setting one", action.getDefinition().getReferenceElement().getIdPart());
                        result.setId( UUID.randomUUID().toString() );
                    }
                    session.getCarePlanBuilder()
                            .buildContained(result)
                            .buildActivity(
                                    new CarePlanActivityBuilder()
                                            .buildReference( new Reference("#"+result.getId()) )
                                            .build()
                            );
                } catch (Exception e) {
                    logger.error("ERROR: ActivityDefinition %s could not be applied and threw exception %s", action.getDefinition(), e.toString());
                }
            }
        }
    }

    private void resolveDynamicActions(Session session, PlanDefinition.PlanDefinitionActionComponent action) {
        for (PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue: action.getDynamicValue())
        {
            logger.info("Resolving dynamic value %s %s", dynamicValue.getPath(), dynamicValue.getExpression());
            if (dynamicValue.hasExpression()) {
                Object result =
                        executionProvider
                                .evaluateInContext(session.getPlanDefinition(), dynamicValue.getExpression(), session.getPatientId());

                if (dynamicValue.hasPath() && dynamicValue.getPath().equals("$this"))
                {
                    session.setCarePlan((CarePlan) result);
                }

                else {

                    // TODO - likely need more date tranformations
                    if (result instanceof DateTime) {
                        result =
                                new JavaDateBuilder()
                                        .buildFromDateTime((DateTime) result)
                                        .build();
                    }

                    else if (result instanceof String) {
                        result = new StringType((String) result);
                    }

                    provider.setValue(session.getCarePlan(), dynamicValue.getPath(), result);
                }
            }
        }
    }

    private Boolean meetsConditions(Session session, PlanDefinition.PlanDefinitionActionComponent action) {
        for (PlanDefinition.PlanDefinitionActionConditionComponent condition: action.getCondition()) {
            // TODO start
            // TODO stop
            if (condition.hasDescription()) {
                logger.info("Resolving condition with description: " + condition.getDescription());
            }
            if (condition.getKind() == PlanDefinition.ActionConditionKind.APPLICABILITY) {
                if (!condition.getLanguage().equals("text/cql")) {
                    logger.warn("An action language other than CQL was found: " + condition.getLanguage());
                    continue;
                }

                if (!condition.hasExpression()) {
                    logger.error("Missing condition expression");
                    throw new RuntimeException("Missing condition expression");
                }

                logger.info("Evaluating action condition expression " + condition.getExpression());
                String cql = condition.getExpression();
                Object result = executionProvider.evaluateInContext(session.getPlanDefinition(), cql, session.getPatientId());

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
    public CarePlan resolveCdsHooksPlanDefinition(Context context, PlanDefinition planDefinition, String patientId) {

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

        resolveActions(planDefinition.getAction(), context, patientId, requestGroupBuilder, new ArrayList<>());

        CarePlanActivityBuilder carePlanActivityBuilder = new CarePlanActivityBuilder();
        carePlanActivityBuilder.buildReferenceTarget(requestGroupBuilder.build());
        carePlanBuilder.buildActivity(carePlanActivityBuilder.build());

        return carePlanBuilder.build();
    }

    private void resolveActions(List<PlanDefinition.PlanDefinitionActionComponent> actions, Context context,
                                String patientId, RequestGroupBuilder requestGroupBuilder,
                                List<RequestGroup.RequestGroupActionComponent> actionComponents)
    {
        for (PlanDefinition.PlanDefinitionActionComponent action : actions) {
            boolean conditionsMet = true;
            for (PlanDefinition.PlanDefinitionActionConditionComponent condition: action.getCondition()) {
                if (condition.getKind() == PlanDefinition.ActionConditionKind.APPLICABILITY) {
                    if (!condition.hasExpression()) {
                        continue;
                    }

                    Object result = context.resolveExpressionRef(condition.getExpression()).getExpression().evaluate(context);

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
                    if (action.hasLabel()) {
                        actionBuilder.buildLabel(action.getLabel());
                    }
                    if (action.hasType()) {
                        actionBuilder.buildType(action.getType());
                    }
                    if (action.hasDefinition()) {
                        if (action.getDefinition().getReferenceElement().getResourceType().equals("ActivityDefinition")) {
                            if (action.getDefinition().getResource() != null) {
                                ActivityDefinition activityDefinition = (ActivityDefinition) action.getDefinition().getResource();
                                ReferenceBuilder referenceBuilder = new ReferenceBuilder();
                                referenceBuilder.buildDisplay(activityDefinition.getDescription());
                                actionBuilder.buildResource(referenceBuilder.build());

                                if (activityDefinition.hasDescription()) {
                                    actionBuilder.buildDescripition(activityDefinition.getDescription());
                                }
                            }

                            // TODO - fix this
                            FHIRActivityDefinitionResourceProvider activitydefinitionProvider = (FHIRActivityDefinitionResourceProvider) provider.resolveResourceProvider("ActivityDefinition");
                            ActivityDefinition activityDefinition =
                                    activitydefinitionProvider.getDao().read(action.getDefinition().getReferenceElement());
                            if (activityDefinition.hasDescription()) {
                                actionBuilder.buildDescripition(activityDefinition.getDescription());
                            }
                            Resource resource = null;
                            try {
                                resource = activitydefinitionProvider.apply(
                                        new IdType(action.getDefinition().getReferenceElement().getIdPart()), patientId,
                                        null, null, null, null,
                                        null, null, null, null
                                ).setId(UUID.randomUUID().toString());
                            } catch (FHIRException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                                throw new RuntimeException("Error applying ActivityDefinition " + e.getMessage());
                            }

//                            BaseFhirDataProvider provider = (BaseFhirDataProvider) context.resolveDataProvider(new QName("http://hl7.org/fhir", ""));
//                            Parameters inParams = new Parameters();
//                            inParams.addParameter().setName("patient").setValue(new StringType(patientId));
//                            Parameters outParams = provider.getFhirClient()
//                                    .operation()
//                                    .onInstance(new IdDt("ActivityDefinition", action.getDefinition().getReferenceElement().getIdPart()))
//                                    .named("$apply")
//                                    .withParameters(inParams)
//                                    .useHttpGet()
//                                    .execute();
//
//                            List<Parameters.ParametersParameterComponent> response = outParams.getParameter();
//                            Resource resource = response.get(0).getResource().setId(UUID.randomUUID().toString());
                            actionBuilder.buildResourceTarget(resource);
                            actionBuilder.buildResource(new ReferenceBuilder().buildReference(resource.getId()).build());
                        }
                    }

                    // Dynamic values populate the RequestGroup - there is a bit of hijacking going on here...
                    if (action.hasDynamicValue()) {
                        for (PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue : action.getDynamicValue()) {
                            if (dynamicValue.hasPath() && dynamicValue.hasExpression()) {
                                if (dynamicValue.getPath().endsWith("title")) { // summary
                                    String title = (String) context.resolveExpressionRef(dynamicValue.getExpression()).evaluate(context);
                                    actionBuilder.buildTitle(title);
                                }
                                else if (dynamicValue.getPath().endsWith("description")) { // detail
                                    String description = (String) context.resolveExpressionRef(dynamicValue.getExpression()).evaluate(context);
                                    actionBuilder.buildDescripition(description);
                                }
                                else if (dynamicValue.getPath().endsWith("extension")) { // indicator
                                    String extension = (String) context.resolveExpressionRef(dynamicValue.getExpression()).evaluate(context);
                                    actionBuilder.buildExtension(extension);
                                }
                            }
                        }
                    }

                    if (!actionBuilder.build().isEmpty()) {
                        actionComponents.add(actionBuilder.build());
                    }

                    if (action.hasAction()) {
                        resolveActions(action.getAction(), context, patientId, requestGroupBuilder, actionComponents);
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

        throw new RuntimeException(String.format("Resource %s does not contain resource with id %s", resource.fhirType(), id));
    }

    public Map<PlanDefinition, Set<String>> getHooks() {
        Map<PlanDefinition, Set<String>> discoveryMap = new HashMap<>();
        IBundleProvider bundleProvider = getDao().search(new SearchParameterMap());
        for (IBaseResource resource : bundleProvider.getResources(0, bundleProvider.size())) {
            if (resource instanceof PlanDefinition) {
                PlanDefinition planDefinition = (PlanDefinition) resource;
                Set<String> urls = getPrefetchUrls(planDefinition);
                if (!urls.isEmpty()) {
                    discoveryMap.put(planDefinition, urls);
                }
            }
        }
        return discoveryMap;
    }

    public Set<String> getPrefetchUrls(PlanDefinition planDefinition) {
        if (planDefinition.hasType()) {
            for (Coding typeCode : planDefinition.getType().getCoding()) {
                if (typeCode.getCode().equals("eca-rule")) {
                    if (planDefinition.hasLibrary()) {
                        for (Reference reference : planDefinition.getLibrary()) {
                            org.cqframework.cql.elm.execution.Library library = libraryLoader.load(
                                    new VersionedIdentifier()
                                            .withId(reference.getReferenceElement().getIdPart())
                                            .withVersion(reference.getReferenceElement().getVersionIdPart())
                            );

                            DiscoveryDataProvider discoveryDataProvider = null;
                            for (UsingDef using : library.getUsings().getDef()) {
                                if (using.getLocalIdentifier().equals("FHIR") && using.getVersion().equals("3.0.0")) {
                                    discoveryDataProvider = new DiscoveryDataProviderStu3();
                                }
                                else if (using.getLocalIdentifier().equals("FHIR") && using.getVersion().equals("1.0.2")) {
                                    discoveryDataProvider = new DiscoveryDataProviderDstu2();
                                }
                            }
                            if (discoveryDataProvider == null) {
                                continue;
                            }
                            discoveryDataProvider.setExpandValueSets(true);
                            discoveryDataProvider.setTerminologyProvider(provider.getTerminologyProvider());
                            Context context = new Context(library);
                            context.registerDataProvider("http://hl7.org/fhir", discoveryDataProvider);
                            context.registerTerminologyProvider(provider.getTerminologyProvider());
                            context.registerLibraryLoader(libraryLoader);
                            context.enterContext("Patient");
                            context.setContextValue(context.getCurrentContext(), "{{context.patientId}}");
                            // TODO - remove once engine issue is resolved
                            if (library.getParameters() != null) {
                                for (ParameterDef params : library.getParameters().getDef()) {
                                    if (params.getParameterTypeSpecifier() instanceof ListTypeSpecifier) {
                                        context.setParameter(null, params.getName(), new ArrayList<>());
                                    }
//                                    else if (params.getParameterTypeSpecifier() instanceof IntervalTypeSpecifier) {
//                                        context.setParameter(null, params.getName(), new Interval(null, true, null, true));
//                                    }
//                                    else if (params.getParameterTypeSpecifier() instanceof TupleTypeSpecifier) {
//                                        context.setParameter(null, params.getName(), new Tuple());
//                                    }
//                                    else {
//                                        context.setParameter(null, params.getName(), new Object());
//                                    }
                                }
                            }
                            for (ExpressionDef def : library.getStatements().getDef()) {
                                try {
                                    def.getExpression().evaluate(context);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            return discoveryDataProvider.getPrefetchUrls();
                        }
                    }
                }
            }
        }
        return Collections.emptySet();
    }

    @Search(allowUnknownParams=true)
    public IBundleProvider search(
            javax.servlet.http.HttpServletRequest theServletRequest,
            RequestDetails theRequestDetails,
            @Description(shortDefinition="Search the contents of the resource's data using a fulltext search")
            @OptionalParam(name=ca.uhn.fhir.rest.api.Constants.PARAM_CONTENT)
                    StringAndListParam theFtContent,
            @Description(shortDefinition="Search the contents of the resource's narrative using a fulltext search")
            @OptionalParam(name=ca.uhn.fhir.rest.api.Constants.PARAM_TEXT)
                    StringAndListParam theFtText,
            @Description(shortDefinition="Search for resources which have the given tag")
            @OptionalParam(name=ca.uhn.fhir.rest.api.Constants.PARAM_TAG)
                    TokenAndListParam theSearchForTag,
            @Description(shortDefinition="Search for resources which have the given security labels")
            @OptionalParam(name=ca.uhn.fhir.rest.api.Constants.PARAM_SECURITY)
                    TokenAndListParam theSearchForSecurity,
            @Description(shortDefinition="Search for resources which have the given profile")
            @OptionalParam(name=ca.uhn.fhir.rest.api.Constants.PARAM_PROFILE)
                    UriAndListParam theSearchForProfile,
            @Description(shortDefinition="Return resources linked to by the given target")
            @OptionalParam(name="_has")
                    HasAndListParam theHas,
            @Description(shortDefinition="The ID of the resource")
            @OptionalParam(name="_id")
                    TokenAndListParam the_id,
            @Description(shortDefinition="The language of the resource")
            @OptionalParam(name="_language")
                    StringAndListParam the_language,
            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="composed-of", targetTypes={  } )
                    ReferenceAndListParam theComposed_of,
            @Description(shortDefinition="The plan definition publication date")
            @OptionalParam(name="date")
                    DateRangeParam theDate,
            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="depends-on", targetTypes={  } )
                    ReferenceAndListParam theDepends_on,
            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="derived-from", targetTypes={  } )
                    ReferenceAndListParam theDerived_from,
            @Description(shortDefinition="The description of the plan definition")
            @OptionalParam(name="description")
                    StringAndListParam theDescription,
            @Description(shortDefinition="The time during which the plan definition is intended to be in use")
            @OptionalParam(name="effective")
                    DateRangeParam theEffective,
            @Description(shortDefinition="External identifier for the plan definition")
            @OptionalParam(name="identifier")
                    TokenAndListParam theIdentifier,
            @Description(shortDefinition="Intended jurisdiction for the plan definition")
            @OptionalParam(name="jurisdiction")
                    TokenAndListParam theJurisdiction,
            @Description(shortDefinition="Computationally friendly name of the plan definition")
            @OptionalParam(name="name")
                    StringAndListParam theName,
            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="predecessor", targetTypes={  } )
                    ReferenceAndListParam thePredecessor,
            @Description(shortDefinition="Name of the publisher of the plan definition")
            @OptionalParam(name="publisher")
                    StringAndListParam thePublisher,
            @Description(shortDefinition="The current status of the plan definition")
            @OptionalParam(name="status")
                    TokenAndListParam theStatus,
            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="successor", targetTypes={  } )
                    ReferenceAndListParam theSuccessor,
            @Description(shortDefinition="The human-friendly name of the plan definition")
            @OptionalParam(name="title")
                    StringAndListParam theTitle,
            @Description(shortDefinition="Topics associated with the module")
            @OptionalParam(name="topic")
                    TokenAndListParam theTopic,
            @Description(shortDefinition="The uri that identifies the plan definition")
            @OptionalParam(name="url")
                    UriAndListParam theUrl,
            @Description(shortDefinition="The business version of the plan definition")
            @OptionalParam(name="version")
                    TokenAndListParam theVersion,
            @RawParam
                    Map<String, List<String>> theAdditionalRawParams,
            @IncludeParam(reverse=true)
                    Set<Include> theRevIncludes,
            @Description(shortDefinition="Only return resources which were last updated as specified by the given range")
            @OptionalParam(name="_lastUpdated")
                    DateRangeParam theLastUpdated,
            @IncludeParam(allow= {
                    "PlanDefinition:composed-of" , 					"PlanDefinition:depends-on" , 					"PlanDefinition:derived-from" , 					"PlanDefinition:predecessor" , 					"PlanDefinition:successor" , 						"PlanDefinition:composed-of" , 					"PlanDefinition:depends-on" , 					"PlanDefinition:derived-from" , 					"PlanDefinition:predecessor" , 					"PlanDefinition:successor" , 						"PlanDefinition:composed-of" , 					"PlanDefinition:depends-on" , 					"PlanDefinition:derived-from" , 					"PlanDefinition:predecessor" , 					"PlanDefinition:successor" , 						"PlanDefinition:composed-of" , 					"PlanDefinition:depends-on" , 					"PlanDefinition:derived-from" , 					"PlanDefinition:predecessor" , 					"PlanDefinition:successor" , 						"PlanDefinition:composed-of" , 					"PlanDefinition:depends-on" , 					"PlanDefinition:derived-from" , 					"PlanDefinition:predecessor" , 					"PlanDefinition:successor" 					, "*"
            })
                    Set<Include> theIncludes,
            @Sort
                    SortSpec theSort,
            @ca.uhn.fhir.rest.annotation.Count
                    Integer theCount
    ) {
        startRequest(theServletRequest);
        try {
            SearchParameterMap paramMap = new SearchParameterMap();
            paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_CONTENT, theFtContent);
            paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_TEXT, theFtText);
            paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_TAG, theSearchForTag);
            paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_SECURITY, theSearchForSecurity);
            paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_PROFILE, theSearchForProfile);
            paramMap.add("_has", theHas);
            paramMap.add("_id", the_id);
            paramMap.add("_language", the_language);
            paramMap.add("composed-of", theComposed_of);
            paramMap.add("date", theDate);
            paramMap.add("depends-on", theDepends_on);
            paramMap.add("derived-from", theDerived_from);
            paramMap.add("description", theDescription);
            paramMap.add("effective", theEffective);
            paramMap.add("identifier", theIdentifier);
            paramMap.add("jurisdiction", theJurisdiction);
            paramMap.add("name", theName);
            paramMap.add("predecessor", thePredecessor);
            paramMap.add("publisher", thePublisher);
            paramMap.add("status", theStatus);
            paramMap.add("successor", theSuccessor);
            paramMap.add("title", theTitle);
            paramMap.add("topic", theTopic);
            paramMap.add("url", theUrl);
            paramMap.add("version", theVersion);
            paramMap.setRevIncludes(theRevIncludes);
            paramMap.setLastUpdated(theLastUpdated);
            paramMap.setIncludes(theIncludes);
            paramMap.setSort(theSort);
            paramMap.setCount(theCount);
//            paramMap.setRequestDetails(theRequestDetails);

            getDao().translateRawParameters(theAdditionalRawParams, paramMap);

            return getDao().search(paramMap, theRequestDetails);
        } finally {
            endRequest(theServletRequest);
        }
    }
}

class Session {
    private final String patientId;
    private final PlanDefinition planDefinition;
    private final String practionerId;
    private final String organizationId;
    private final String userType;
    private final String userLanguage;
    private final String userTaskContext;
    private final String setting;
    private final String settingContext;
    private CarePlanBuilder carePlanBuilder;
    private String encounterId;

    public Session(PlanDefinition planDefinition, CarePlanBuilder builder, String patientId, String encounterId,
                   String practitionerId, String organizationId, String userType, String userLanguage,
                   String userTaskContext, String setting, String settingContext)
    {
        this.patientId = patientId;
        this.planDefinition = planDefinition;
        this.carePlanBuilder = builder;
        this.encounterId = encounterId;
        this.practionerId = practitionerId;
        this.organizationId = organizationId;
        this.userType = userType;
        this.userLanguage = userLanguage;
        this.userTaskContext = userTaskContext;
        this.setting = setting;
        this.settingContext = settingContext;
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

    public String getPractionerId() {
        return practionerId;
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
}
