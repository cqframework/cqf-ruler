package org.opencds.cqf.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.QuantityDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelInfoLoader;
import org.cqframework.cql.cql2elm.ModelInfoProvider;
import org.cqframework.cql.elm.execution.ExpressionDef;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.elm_modelinfo.r1.ModelInfo;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.cql.data.fhir.FhirDataProvider;
import org.opencds.cqf.cql.data.fhir.JpaFhirDataProvider;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.CqlLibraryReader;
import org.opencds.cqf.cql.execution.LibraryLoader;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Bryn on 1/16/2017.
 */
public class PlanDefinitionResourceProvider extends JpaResourceProviderDstu3<PlanDefinition> {
    private JpaFhirDataProvider provider;
    private CqlExecutionProvider executionProvider;

    public PlanDefinitionResourceProvider(Collection<IResourceProvider> providers) {
        this.provider = new JpaFhirDataProvider(providers);
        this.executionProvider = new CqlExecutionProvider(providers);
    }

//    private ModelManager modelManager;
//    private ModelManager getModelManager() {
//        if (modelManager == null) {
//            modelManager = new ModelManager();
//            ModelInfoProvider infoProvider = () -> {
//                Path p = Paths.get("src/main/resources/OMTK-modelinfo-0.1.0.xml").toAbsolutePath();
//                return JAXB.unmarshal(new File(p.toString()), ModelInfo.class);
//            };
//            ModelInfoLoader.registerModelInfoProvider(new VersionedIdentifier().withId("OMTK").withVersion("0.1.0"), infoProvider);
//        }
//        return modelManager;
//    }

    private LibraryManager libraryManager;
    private LibraryManager getLibraryManager() {
        if (libraryManager == null) {
            libraryManager = new LibraryManager();
            libraryManager.getLibrarySourceLoader().clearProviders();
            libraryManager.getLibrarySourceLoader().registerProvider(getLibrarySourceProvider());
            ModelInfoProvider infoProvider = () -> {
                Path p = Paths.get("src/main/resources/OMTK-modelinfo-0.1.0.xml").toAbsolutePath();
                return JAXB.unmarshal(new File(p.toString()), ModelInfo.class);
            };
            ModelInfoLoader.registerModelInfoProvider(new VersionedIdentifier().withId("OMTK").withVersion("0.1.0"), infoProvider);
        }
        return libraryManager;
    }

    private LibraryLoader libraryLoader;
    private LibraryLoader getLibraryLoader() {
        if (libraryLoader == null) {
            libraryLoader = new MeasureLibraryLoader(getLibraryResourceProvider(), getLibraryManager());
        }
        return libraryLoader;
    }

    private MeasureLibrarySourceProvider librarySourceProvider;
    private MeasureLibrarySourceProvider getLibrarySourceProvider() {
        if (librarySourceProvider == null) {
            librarySourceProvider = new MeasureLibrarySourceProvider(getLibraryResourceProvider());
        }
        return librarySourceProvider;
    }

    private LibraryResourceProvider getLibraryResourceProvider() {
        return (LibraryResourceProvider)provider.resolveResourceProvider("Library");
    }

//    @Operation(name = "$apply", idempotent = false)
//    public CarePlan apply(@IdParam IdType theId, @RequiredParam(name="patient") String patientId,
//                          @OptionalParam(name="encounter") String encounterId,
//                          @OptionalParam(name="practitioner") String practitionerId,
//                          @OptionalParam(name="organization") String organizationId,
//                          @OptionalParam(name="userType") String userType,
//                          @OptionalParam(name="userLanguage") String userLanguage,
//                          @OptionalParam(name="userTaskContext") String userTaskContext,
//                          @OptionalParam(name="setting") String setting,
//                          @OptionalParam(name="settingContext") String settingContext)
//            throws InternalErrorException, FHIRException {
//        PlanDefinition planDefinition = this.getDao().read(theId);
//
//        CarePlan result = new CarePlan();
//        return result;
//    }

    // TODO: include the params in the apply op above where to put source?
    @Operation(name = "$apply", idempotent = true)
    public CarePlan apply(@IdParam IdType theId, @RequiredParam(name="patient") String patientId,
                           @OptionalParam(name="source") String fhirEndpoint) throws IOException, JAXBException, FHIRException
    {
        PlanDefinition planDefinition = this.getDao().read(theId);

        org.cqframework.cql.elm.execution.Library library;

        if (planDefinition.getLibrary() == null || planDefinition.getLibrary().isEmpty()) {
            // get default
            library = CqlLibraryReader.read(new File(Paths.get("src/main/resources/OpioidCDS_STU3-0.1.0.xml").toAbsolutePath().toString()));
        }

        else {
            // TODO: fix this...
            // fetch it
            String ref = planDefinition.getLibrary().get(0).getReference();

            if (ref.contains("http://") || ref.contains("https://")) {
                // full url reference
                library = (Library) provider.getFhirClient().read(new UriDt(ref));
            }

            else if (ref.contains("Library/")) {
                // Id with resource
                library = (Library) provider.getFhirClient()
                        .read(new UriDt("http://fhirtest.uhn.ca/baseDstu2" + ref));
            }

            else {
                library = (Library) provider.getFhirClient()
                        .read(new UriDt("http://fhirtest.uhn.ca/baseDstu2/Library/" + ref));
            }
        }

        // resolve data provider
        String path = Paths.get("src/main/resources/OpioidManagementTerminologyKnowledge.accdb").toAbsolutePath().toString().replace("\\", "/");
        String connString = "jdbc:ucanaccess://" + path + ";memory=false;keepMirror=true";
        OmtkDataProvider omtkProvider = new OmtkDataProvider(connString);
        FhirDataProvider dstu3Provider = new FhirDataProvider().withEndpoint("http://measure.eval.kanvix.com/cqf-ruler/baseDstu3");
        Bundle bundle = FhirContext.forDstu2().newRestfulGenericClient(fhirEndpoint).search().byUrl("MedicationOrder?patient=" + patientId).returnBundle(Bundle.class).execute();

        List<MedicationRequest> requests = convertFromDstu2(bundle);

        Context context = new Context(library);
        context.registerLibraryLoader(getLibraryLoader());
        context.registerDataProvider("http://org.opencds/opioid-cds", omtkProvider);
        context.registerDataProvider("http://hl7.org/fhir", dstu3Provider);
        context.setExpressionCaching(true);

        // walk through plandefintion actions
        // TODO: implement for suggestions and source and dynamicValue
        CarePlan careplan = new CarePlan();
        for (PlanDefinition.PlanDefinitionActionComponent action : planDefinition.getAction())
        {
            String title = action.hasTitle() ? action.getTitle() : null;
            String description = action.hasDescription() ? action.getDescription() : null;

            if (action.hasCondition()) {
                for (PlanDefinition.PlanDefinitionActionConditionComponent condition : action.getCondition()) {

                    // execute condition if applicable
                    if (condition.getKind().toCode().equals("applicability") && condition.hasExpression()) {

                        // if (condition.hasLanguage() && condition.getLanguage().equals("text/cql")) {}

                        // run the expression
                        ExpressionDef def = context.resolveExpressionRef(condition.getExpression());
                        context.setParameter(null, "Orders", requests);
                        Object result = def.getExpression().evaluate(context);

                        if (!(result instanceof Boolean)) {
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

    private List<MedicationRequest> convertFromDstu2(Bundle bundle) throws FHIRException {
        List<MedicationRequest> requests = new ArrayList<>();

        for (Bundle.Entry entry : bundle.getEntry()) {
            MedicationOrder order = (MedicationOrder) entry.getResource();
            requests.add(convertToMedicationRequest(order));
        }

        return requests;
    }

    private MedicationRequest convertToMedicationRequest(MedicationOrder order) throws FHIRException {
        /*
        * Required fields:
        * MedicationOrder -> MedicationRequest
        * medication -> medication
        * dispenseRequest (Backbone) -> Dosage (Element)
        * */
        return new MedicationRequest()
                .setStatus(MedicationRequest.MedicationRequestStatus.fromCode(order.getStatus()))
                .setMedication(convertToCodeableConcept((CodeableConceptDt) order.getMedication()))
                .setDosageInstruction(convertToDosage(order.getDosageInstruction()));
    }

    private CodeableConcept convertToCodeableConcept(CodeableConceptDt conceptDt) {
        CodeableConcept concept = new CodeableConcept().setText(conceptDt.getText());
        concept.setId(conceptDt.getElementSpecificId());
        List<Coding> codes = new ArrayList<>();
        for (CodingDt code : conceptDt.getCoding()) {
            codes.add(new Coding()
                        .setCode(code.getCode())
                        .setSystem(code.getSystem())
                        .setDisplay(code.getDisplay())
                        .setVersion(code.getVersion())
            );
        }
        return concept.setCoding(codes);
    }

    public List<Dosage> convertToDosage(List<MedicationOrder.DosageInstruction> instructions) throws FHIRException {
        List<Dosage> dosages = new ArrayList<>();

        for (MedicationOrder.DosageInstruction dosageInstruction : instructions) {
            Dosage dosage = new Dosage();
            dosage.setText(dosageInstruction.getText());
            dosage.setAsNeeded(new BooleanType(((BooleanDt) dosageInstruction.getAsNeeded()).getValue()));

            Timing timing = new Timing();
            Timing.TimingRepeatComponent repeat = new Timing.TimingRepeatComponent()
                    .setFrequency(dosageInstruction.getTiming().getRepeat().getFrequency())
                    .setFrequencyMax(dosageInstruction.getTiming().getRepeat().getFrequencyMax())
                    .setPeriod(dosageInstruction.getTiming().getRepeat().getPeriod())
                    .setPeriodUnit(Timing.UnitsOfTime.fromCode(dosageInstruction.getTiming().getRepeat().getPeriodUnits()));

            timing.setRepeat(repeat);
            dosage.setTiming(timing);

            QuantityDt quantityDt = (QuantityDt) dosageInstruction.getDose();
            dosage.setDose(new Quantity()
                            .setValue(quantityDt.getValue())
                            .setUnit(quantityDt.getUnit())
                            .setCode(quantityDt.getCode())
                            .setSystem(quantityDt.getSystem())
            );

            dosages.add(dosage);
        }

        return dosages;
    }
}
