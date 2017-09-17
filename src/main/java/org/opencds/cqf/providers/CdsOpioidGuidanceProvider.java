package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.method.RequestDetails;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IBundleProvider;
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
public class CdsOpioidGuidanceProvider {
    private JpaDataProvider provider;
    private CqlExecutionProvider executionProvider;

    public CdsOpioidGuidanceProvider(Collection<IResourceProvider> providers) {
        this.provider = new JpaDataProvider(providers);
        this.executionProvider = new CqlExecutionProvider(providers);
    }

    public CdsOpioidGuidanceProvider(JpaDataProvider provider) {
        this.provider = provider;
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

    public CarePlan applyCdsOpioidGuidance(IdType theId, String patientId, String encounterId,
                                           String practitionerId, String organizationId, String userType,
                                           String userLanguage, String userTaskContext, String setting,
                                           String settingContext, Parameters contextParams)
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
        PlanDefinition planDefinition =
                (PlanDefinition) provider
                        .resolveResourceProvider("PlanDefinition")
                        .getDao()
                        .read(theId);

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

}
