package org.opencds.cqf.cds;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import com.google.gson.JsonElement;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.config.STU3LibrarySourceProvider;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.exceptions.MissingContextException;
import org.opencds.cqf.helpers.Dstu2ToStu3;

import java.util.ArrayList;
import java.util.List;

public class MedicationPrescribeProcessor extends CdsRequestProcessor {

    MedicationRequest contextPrescription;
    List<MedicationRequest> activePrescriptions;

    private ModelManager modelManager;
    private ModelManager getModelManager() {
        if (modelManager == null) {
            modelManager = new ModelManager();
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

    public MedicationPrescribeProcessor(CdsHooksRequest request, PlanDefinition planDefinition, LibraryResourceProvider libraryResourceProvider)
            throws FHIRException
    {
        super(request, planDefinition, libraryResourceProvider);

        this.activePrescriptions = new ArrayList<>();
        resolveContextPrescription();
        resolveActivePrescriptions();
    }

    @Override
    public List<CdsCard> process() {
        Library library = getLibraryLoader().load(new org.cqframework.cql.elm.execution.VersionedIdentifier().withId("OpioidCdsStu3").withVersion("0.1.0"));

        // TODO - make it so user can set this.
        BaseFhirDataProvider dstu3Provider = new FhirDataProviderStu3().setEndpoint("http://measure.eval.kanvix.com/cqf-ruler/baseDstu3");

        Context executionContext = new Context(library);
        executionContext.registerLibraryLoader(getLibraryLoader());
        executionContext.registerDataProvider("http://hl7.org/fhir", dstu3Provider);
        executionContext.setExpressionCaching(true);
//        executionContext.setEnableTraceLogging(true);
        executionContext.setParameter(null, "Orders", activePrescriptions);

        return resolveActions(executionContext);
    }

    private void resolveContextPrescription() throws FHIRException {
        if (request.getContext().size() == 0) {
            throw new MissingContextException("The medication-prescribe request requires the context to contain a prescription order.");
        }

        String resourceName = request.getContext().getAsJsonObject().getAsJsonPrimitive("resourceType").getAsString();
        this.contextPrescription = getMedicationRequest(resourceName, request.getContext().toString());
    }

    private void resolveActivePrescriptions() throws FHIRException {
        this.activePrescriptions.add(contextPrescription); // include the context prescription
        String resourceName;
        for (JsonElement element : request.getPrefetch().getAsJsonObject("medication").getAsJsonArray("resources")) {
            resourceName = element.getAsJsonObject().getAsJsonPrimitive("resourceType").getAsString();
            this.activePrescriptions.add(getMedicationRequest(resourceName, element.toString()));
        }
    }

    private MedicationRequest getMedicationRequest(String resourceName, String resource) throws FHIRException {
        if (resourceName.equals("MedicationOrder")) {
            MedicationOrder order = (MedicationOrder) FhirContext.forDstu2().newJsonParser().parseResource(resource);
            return Dstu2ToStu3.resolveMedicationRequest(order);
        }

        return (MedicationRequest) FhirContext.forDstu3().newJsonParser().parseResource(resource);
    }
}
