package org.opencds.cqf.cds;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelInfoLoader;
import org.cqframework.cql.cql2elm.ModelInfoProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.config.STU3LibrarySourceProvider;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.opioidcds.OmtkDataProvider;
import org.opencds.cqf.opioidcds.OmtkModelInfoProvider;
import org.opencds.cqf.providers.FHIRPlanDefinitionResourceProvider;

import java.nio.file.Paths;
import java.util.List;

public class OpioidGuidanceProcessor extends MedicationPrescribeProcessor {

    private ModelManager modelManager;
    private ModelManager getModelManager() {
        if (modelManager == null) {
            modelManager = new ModelManager();
            ModelInfoProvider infoProvider = new OmtkModelInfoProvider();
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
            libraryLoader = new STU3LibraryLoader(libraryResourceProvider, getLibraryManager(), getModelManager());
        }
        return libraryLoader;
    }

    private STU3LibrarySourceProvider librarySourceProvider;
    private STU3LibrarySourceProvider getLibrarySourceProvider() {
        if (librarySourceProvider == null) {
            librarySourceProvider = new STU3LibrarySourceProvider(libraryResourceProvider);
        }
        return librarySourceProvider;
    }

    public OpioidGuidanceProcessor(CdsHooksRequest request, LibraryResourceProvider libraryResourceProvider,
                                   FHIRPlanDefinitionResourceProvider planDefinitionResourceProvider, boolean isStu3)
            throws FHIRException
    {
        super(request, libraryResourceProvider, planDefinitionResourceProvider, isStu3);
    }

    @Override
    public List<CdsCard> process() {

        // validate resources
        validateContextAndPrefetchResources(contextPrescription);
        for (MedicationRequest request : activePrescriptions) {
            validateContextAndPrefetchResources(request);
        }

        // read opioid library
        Library library = getLibraryLoader().load(new org.cqframework.cql.elm.execution.VersionedIdentifier().withId("OpioidCdsStu3").withVersion("0.1.0"));

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        // resolve data providers
        // the db file is issued to properly licensed implementers -- see README for more info
        String path = Paths.get("src/main/resources/cds/LocalDataStore_RxNav_OpioidCds.db").toAbsolutePath().toString().replace("\\", "/");
        String connString = "jdbc:sqlite://" + path;
        OmtkDataProvider omtkProvider = new OmtkDataProvider(connString);
        BaseFhirDataProvider dstu3Provider = new FhirDataProviderStu3().setEndpoint(request.getFhirServerEndpoint());

        Context executionContext = new Context(library);
        executionContext.registerLibraryLoader(getLibraryLoader());
        executionContext.registerDataProvider("http://org.opencds/opioid-cds", omtkProvider);
        executionContext.registerDataProvider("http://hl7.org/fhir", dstu3Provider);
        executionContext.setExpressionCaching(true);
        executionContext.setParameter(null, "Orders", activePrescriptions);

        List<CdsCard> cards = resolveActions(executionContext);
        if (cards.isEmpty() || (cards.size() == 1 && !cards.get(0).hasDetail() && !cards.get(0).hasSummary() && !cards.get(0).hasIndicator())) {
            cards.add(
                    new CdsCard()
                            .setSummary("Success")
                            .setDetail("Prescription satisfies recommendation #5 of the cdc opioid guidance.")
                            .setIndicator("info")
            );
        }

        return cards;
    }

    // TODO - finish this
    private void validateContextAndPrefetchResources(MedicationRequest prescription) {
        if (!prescription.hasMedication()) {
            throw new RuntimeException("Missing medication code in prescrition " + prescription.getId());
        }

        if (prescription.hasDosageInstruction()) {
            if (!prescription.getDosageInstructionFirstRep().hasAsNeededBooleanType()) {
                throw new RuntimeException("Missing/invalid asNeededBoolean field in dosageInstruction for prescription " + prescription.getId());
            }
            if (!prescription.getDosageInstructionFirstRep().hasDoseSimpleQuantity()) {
                throw new RuntimeException("Missing/invalid doseQuantity field in dosageInstruction for prescription " + prescription.getId());
            }
        }
        else {
            throw new RuntimeException("Missing dosageInstruction structure in prescription " + prescription.getId());
        }
    }
}
