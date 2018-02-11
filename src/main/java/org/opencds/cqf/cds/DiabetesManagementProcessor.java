package org.opencds.cqf.cds;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Resource;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.providers.FHIRPlanDefinitionResourceProvider;

import java.util.ArrayList;
import java.util.List;

public class DiabetesManagementProcessor extends PatientViewProcessor {

    public DiabetesManagementProcessor(CdsHooksRequest request, LibraryResourceProvider libraryResourceProvider, FHIRPlanDefinitionResourceProvider planDefinitionResourceProvider, boolean isStu3) {
        super(request, libraryResourceProvider, planDefinitionResourceProvider, isStu3);
    }

    @Override
    public List<CdsCard> process() {
        Library library = providers.getLibraryLoader().load(new org.cqframework.cql.elm.execution.VersionedIdentifier().withId("diabetes-management"));
        BaseFhirDataProvider dstu3Provider = new FhirDataProviderStu3().setEndpoint(request.getFhirServerEndpoint());
        // TODO - assuming terminology service is same as data provider - not a great assumption...
        dstu3Provider.setTerminologyProvider(new FhirTerminologyProvider().withEndpoint(request.getFhirServerEndpoint()));
        dstu3Provider.setExpandValueSets(true);

        Context executionContext = new Context(library);
        executionContext.registerLibraryLoader(providers.getLibraryLoader());
        executionContext.registerDataProvider("http://hl7.org/fhir", dstu3Provider);
        executionContext.registerTerminologyProvider(dstu3Provider.getTerminologyProvider());
        executionContext.setContextValue("Patient", request.getPatientId());
        executionContext.setExpressionCaching(true);
        resolveParams(executionContext);

        return resolveActions(executionContext);
    }

    private void resolveParams(Context context) {
        for (String key : request.getPrefetch().keySet()) {
            Bundle bundle = (org.hl7.fhir.dstu3.model.Bundle) FhirContext.forDstu3().newJsonParser().parseResource(request.getPrefetch().getAsJsonObject(key).getAsJsonObject("resource").toString());
            context.setParameter(null, key, resolveEntry(bundle));
        }
    }

    private List<Resource> resolveEntry(Bundle bundle) {
        List<Resource> resources = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource()) {
                resources.add(entry.getResource());
            }
        }
        return resources;
    }
}
