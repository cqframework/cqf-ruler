package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelInfoLoader;
import org.cqframework.cql.cql2elm.ModelInfoProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.ExpressionDef;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.elm_modelinfo.r1.ModelInfo;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.config.STU3LibrarySourceProvider;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.CqlLibraryReader;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.runtime.Tuple;
import org.opencds.cqf.omtk.OmtkDataProvider;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by Bryn on 1/16/2017.
 */
public class PlanDefinitionResourceProvider extends JpaResourceProviderDstu3<PlanDefinition> {
    private JpaDataProvider provider;
    private CqlExecutionProvider executionProvider;

    public PlanDefinitionResourceProvider(Collection<IResourceProvider> providers) {
        this.provider = new JpaDataProvider(providers);
        this.executionProvider = new CqlExecutionProvider(providers);
    }

    private ModelManager modelManager;
    private ModelManager getModelManager() {
        if (modelManager == null) {
            modelManager = new ModelManager();
            ModelInfoProvider infoProvider = () -> {
                Path p = Paths.get("src/main/resources/cds/OMTK-modelinfo-0.1.0.xml").toAbsolutePath();
                return JAXB.unmarshal(new File(p.toString()), ModelInfo.class);
            };
            ModelInfoLoader.registerModelInfoProvider(new VersionedIdentifier().withId("OMTK").withVersion("0.1.0"), infoProvider);
        }
        return modelManager;
    }

    private LibraryManager libraryManager;
    private LibraryManager getLibraryManager() {
        if (libraryManager == null) {
            libraryManager = new LibraryManager(getModelManager());
            libraryManager.getLibrarySourceLoader().clearProviders();
            libraryManager.getLibrarySourceLoader().registerProvider(getLibrarySourceProvider());
            ModelInfoProvider infoProvider = () -> {
                Path p = Paths.get("src/main/resources/cds/OMTK-modelinfo-0.1.0.xml").toAbsolutePath();
                return JAXB.unmarshal(new File(p.toString()), ModelInfo.class);
            };
            ModelInfoLoader.registerModelInfoProvider(new VersionedIdentifier().withId("OMTK").withVersion("0.1.0"), infoProvider);
        }
        return libraryManager;
    }

    private LibraryLoader libraryLoader;
    private LibraryLoader getLibraryLoader() {
        if (libraryLoader == null) {
            libraryLoader = new STU3LibraryLoader(getLibraryResourceProvider(), getLibraryManager(), getModelManager());
        }
        return libraryLoader;
    }

    private STU3LibrarySourceProvider librarySourceProvider;
    private STU3LibrarySourceProvider getLibrarySourceProvider() {
        if (librarySourceProvider == null) {
            librarySourceProvider = new STU3LibrarySourceProvider(getLibraryResourceProvider());
        }
        return librarySourceProvider;
    }

    private LibraryResourceProvider getLibraryResourceProvider() {
        return (LibraryResourceProvider)provider.resolveResourceProvider("Library");
    }

    // This is basically designed to run the cdc opioid plan definition
    // TODO: Generify this logic to run any plan definition...
    @Operation(name = "$apply", idempotent = true)
    public CarePlan apply(@IdParam IdType theId, @RequiredParam(name="patient") String patientId,
                          @OptionalParam(name="encounter") String encounterId,
                          @OptionalParam(name="practitioner") String practitionerId,
                          @OptionalParam(name="organization") String organizationId,
                          @OptionalParam(name="userType") String userType,
                          @OptionalParam(name="userLanguage") String userLanguage,
                          @OptionalParam(name="userTaskContext") String userTaskContext,
                          @OptionalParam(name="setting") String setting,
                          @OptionalParam(name="settingContext") String settingContext,
                          @ResourceParam Parameters contextParams)
            throws IOException, JAXBException, FHIRException
    {
        // parse params
        Bundle prefetch = null;
        Bundle medicationReqs = null;
        String source = null;
        for (Parameters.ParametersParameterComponent param : contextParams.getParameter()) {
            if (param.getName().equals("context")) {
                for (Parameters.ParametersParameterComponent component : ((Parameters) param.getResource()).getParameter()) {
                    switch (component.getName()) {
                        case "prefetch": prefetch = (Bundle) component.getResource(); break;
                        case "contextResources": medicationReqs = (Bundle) component.getResource(); break;
                        case "source": source = component.getValue().primitiveValue(); break;
                    }
                }
            }
        }

        // read opioid library
        org.cqframework.cql.elm.execution.Library library =
                CqlLibraryReader.read(new File(
                        Paths.get("src/main/resources/cds/OpioidCDS_STU3-0.1.0.xml").toAbsolutePath().toString()
                ));

        // resolve data providers
        // the db file is issued to properly licensed implementers -- see README for more info
        String path = Paths.get("src/main/resources/cds/OpioidManagementTerminologyKnowledge.db").toAbsolutePath().toString().replace("\\", "/");
        String connString = "jdbc:sqlite://" + path;
        OmtkDataProvider omtkProvider = new OmtkDataProvider(connString);
        BaseFhirDataProvider dstu3Provider = new FhirDataProviderStu3().setEndpoint("http://measure.eval.kanvix.com/cqf-ruler/baseDstu3");

        Context context = new Context(library);
        context.registerLibraryLoader(getLibraryLoader());
        context.registerDataProvider("http://org.opencds/opioid-cds", omtkProvider);
        context.registerDataProvider("http://hl7.org/fhir", dstu3Provider);
        context.setExpressionCaching(true);
        context.setEnableTraceLogging(true);

        // prepare List of MedicationRequests
        List<MedicationRequest> requests = new ArrayList<>();
        if (prefetch != null) {
            for (Bundle.BundleEntryComponent entry : prefetch.getEntry()) {
                requests.add((MedicationRequest) entry.getResource());
            }
        }
        if (medicationReqs != null) {
            for (Bundle.BundleEntryComponent entry : medicationReqs.getEntry()) {
                requests.add((MedicationRequest) entry.getResource());
            }
        }

        // fetch PlanDefinition
        PlanDefinition planDefinition = this.getDao().read(theId);

        return resolveOpioidMedicationPlanDefinition(context, requests, planDefinition);
    }

    private CarePlan resolveOpioidMedicationPlanDefinition(Context context, List<MedicationRequest> requests, PlanDefinition planDefinition) {
        // walk through plandefintion actions
        // TODO: implement for suggestions and source and dynamicValue
        CarePlan careplan = new CarePlan();
        String title;
        String description;
        for (PlanDefinition.PlanDefinitionActionComponent action : planDefinition.getAction())
        {
            title = action.hasTitle() ? action.getTitle() : null;
            description = action.hasDescription() ? action.getDescription() : null;

            if (action.hasCondition()) {
                for (PlanDefinition.PlanDefinitionActionConditionComponent condition : action.getCondition()) {

                    // execute condition if applicable
                    if (condition.getKind().toCode().equals("applicability") && condition.hasExpression()) {

                        // if (condition.hasLanguage() && condition.getLanguage().equals("text/cql")) {}

                        // run the expression
                        ExpressionDef def = context.resolveExpressionRef(condition.getExpression());
                        context.setParameter(null, "Orders", requests);
                        Object result = def.getExpression().evaluate(context);

                        if (result instanceof Tuple) {
                            title = (String) ((Tuple) result).getElement("title");
                            description = (String) ((Tuple) result).getElement("description");
                            result = ((Tuple) result).getElement("mmeOver50");
                        }

                        else {
                            result = false;
                        }

                        if ((Boolean) result) {
                            careplan.setStatus(CarePlan.CarePlanStatus.ACTIVE)
                                    .setIntent(CarePlan.CarePlanIntent.ORDER);

                            if (title != null) careplan.setTitle(title);
                            if (description != null) careplan.setDescription(description);

                            List<CarePlan.CarePlanActivityComponent> activities = new ArrayList<>();
                            CarePlan.CarePlanActivityComponent activity = new CarePlan.CarePlanActivityComponent();
                            CarePlan.CarePlanActivityDetailComponent detail = new CarePlan.CarePlanActivityDetailComponent();
                            detail.setStatus(CarePlan.CarePlanActivityStatus.INPROGRESS);
                            detail.setStatusReason("warning");

                            // get any links from documentation
                            List<Annotation> annotations = new ArrayList<>();
                            for (RelatedArtifact artifact : action.getDocumentation()) {
                                if (artifact.hasUrl()) {
                                    Annotation urls = new Annotation().setText(artifact.getUrl());
                                    if (artifact.hasDisplay()) {
                                        urls.setId(artifact.getDisplay());
                                    }
                                    annotations.add(urls);
                                }
                            }
                            careplan.setNote(annotations);
                            activity.setDetail(detail);
                            activities.add(activity);
                            careplan.setActivity(activities);
                        }

                        else {
                            careplan.setStatus(CarePlan.CarePlanStatus.COMPLETED);
                            careplan.setIntent(CarePlan.CarePlanIntent.ORDER);
                            if (title != null) careplan.setTitle(title);
                            if (description != null) careplan.setDescription(description);
                        }
                    }
                }
            }
        }
        return careplan;
    }

    @Search(allowUnknownParams=true)
    public IBundleProvider search(
            javax.servlet.http.HttpServletRequest theServletRequest,
            RequestDetails theRequestDetails,
            @Description(shortDefinition="Search the contents of the resource's data using a fulltext search")
            @OptionalParam(name=Constants.PARAM_CONTENT)
            StringAndListParam theFtContent,
            @Description(shortDefinition="Search the contents of the resource's narrative using a fulltext search")
            @OptionalParam(name=Constants.PARAM_TEXT)
            StringAndListParam theFtText,
            @Description(shortDefinition="Search for resources which have the given tag")
            @OptionalParam(name=Constants.PARAM_TAG)
            TokenAndListParam theSearchForTag,
            @Description(shortDefinition="Search for resources which have the given security labels")
            @OptionalParam(name=Constants.PARAM_SECURITY)
            TokenAndListParam theSearchForSecurity,
            @Description(shortDefinition="Search for resources which have the given profile")
            @OptionalParam(name=Constants.PARAM_PROFILE)
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
            paramMap.add(Constants.PARAM_CONTENT, theFtContent);
            paramMap.add(Constants.PARAM_TEXT, theFtText);
            paramMap.add(Constants.PARAM_TAG, theSearchForTag);
            paramMap.add(Constants.PARAM_SECURITY, theSearchForSecurity);
            paramMap.add(Constants.PARAM_PROFILE, theSearchForProfile);
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
