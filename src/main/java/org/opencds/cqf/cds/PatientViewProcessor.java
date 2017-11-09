package org.opencds.cqf.cds;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.config.STU3LibrarySourceProvider;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;

import java.util.List;

public class PatientViewProcessor extends CdsRequestProcessor {

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

    public PatientViewProcessor(CdsHooksRequest request, PlanDefinition planDefinition, LibraryResourceProvider libraryResourceProvider) {
        super(request, planDefinition, libraryResourceProvider);
    }

    @Override
    public List<CdsCard> process() {
        // TODO - need a better way to determine library id
        Library library = getLibraryLoader().load(new org.cqframework.cql.elm.execution.VersionedIdentifier().withId("patient-view"));

        // Retrieve FHIR server endpoint from CDS Hooks request, or default to localhost
        String fhirServerEndpoint = request.getFhirServerEndpoint();
        if (fhirServerEndpoint == null) {
        		fhirServerEndpoint = "http://localhost:8080/cqf-ruler/baseDstu3";
        }
        
        // TODO - what are requirements for a valid terminology provider endpoint?  Fails with HSPC sandbox.
//        String terminologyProvider = "http://measure.eval.kanvix.com/cqf-ruler/baseDstu3";
        
        BaseFhirDataProvider dstu3Provider = new FhirDataProviderStu3().setEndpoint(fhirServerEndpoint);
        dstu3Provider.setTerminologyProvider(new FhirTerminologyProvider().withEndpoint(fhirServerEndpoint));
        dstu3Provider.setExpandValueSets(true);

        Context executionContext = new Context(library);
        executionContext.registerLibraryLoader(getLibraryLoader());
        executionContext.registerDataProvider("http://hl7.org/fhir", dstu3Provider);
        executionContext.registerTerminologyProvider(dstu3Provider.getTerminologyProvider());
        executionContext.setContextValue("Patient", request.getPatientId());
        executionContext.setExpressionCaching(true);
//        executionContext.setEnableTraceLogging(true);

        return resolveActions(executionContext);
    }
}
