package org.opencds.cqf.cds;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.exceptions.MissingContextException;
import org.opencds.cqf.helpers.Dstu2ToStu3;
import org.opencds.cqf.providers.FHIRPlanDefinitionResourceProvider;

import java.util.ArrayList;
import java.util.List;

public class MedicationPrescribeProcessor extends CdsRequestProcessor {

    MedicationRequest contextPrescription;
    List<MedicationRequest> activePrescriptions;

    public MedicationPrescribeProcessor(CdsHooksRequest request, LibraryResourceProvider libraryResourceProvider,
                                        FHIRPlanDefinitionResourceProvider planDefinitionResourceProvider, boolean isStu3)
            throws FHIRException
    {
        super(request, libraryResourceProvider, planDefinitionResourceProvider, isStu3);

        this.activePrescriptions = new ArrayList<>();
        resolveContextPrescription();
        resolveActivePrescriptions();
    }

    @Override
    public List<CdsCard> process() {
        // TODO - need a better way to determine library id
        Library library = providers.getLibraryLoader().load(new org.cqframework.cql.elm.execution.VersionedIdentifier().withId("medication-prescribe"));

        BaseFhirDataProvider dstu3Provider = new FhirDataProviderStu3().setEndpoint(request.getFhirServerEndpoint());
        // TODO - assuming terminology service is same as data provider - not a great assumption...
        dstu3Provider.setTerminologyProvider(new FhirTerminologyProvider().withEndpoint(request.getFhirServerEndpoint()));
        dstu3Provider.setExpandValueSets(true);

        Context executionContext = new Context(library);
        executionContext.registerLibraryLoader(providers.getLibraryLoader());
        executionContext.registerDataProvider("http://hl7.org/fhir", dstu3Provider);
        executionContext.setExpressionCaching(true);
        executionContext.setParameter(null, "Orders", activePrescriptions);

        return resolveActions(executionContext);
    }

    private void resolveContextPrescription() throws FHIRException {
        if (request.getContext().size() == 0) {
            throw new MissingContextException("The medication-prescribe request requires the context to contain a prescription order.");
        }

        String resourceName = request.getContext().getAsJsonPrimitive("resourceType").getAsString();
        if (!isStu3) {
            this.contextPrescription = getMedicationRequest(resourceName, FhirContext.forDstu2().newJsonParser().parseResource(request.getContext().toString()));
        }
        else {
            this.contextPrescription = getMedicationRequest(resourceName, FhirContext.forDstu3().newJsonParser().parseResource(request.getContext().toString()));
        }
    }

    private void resolveActivePrescriptions() throws FHIRException {
        this.activePrescriptions.add(contextPrescription); // include the context prescription

        if (!isStu3) {
            Bundle bundle = (Bundle) FhirContext.forDstu2().newJsonParser().parseResource(request.getPrefetch().getAsJsonObject("medication").getAsJsonObject("resource").toString());
            if (bundle.getEntry() == null) {
                return;
            }
            for (Bundle.Entry entry : bundle.getEntry()) {
                this.activePrescriptions.add(getMedicationRequest(entry.getResource().getResourceName(), entry.getResource()));
            }
        }
        else {
            org.hl7.fhir.dstu3.model.Bundle bundle = (org.hl7.fhir.dstu3.model.Bundle) FhirContext.forDstu3().newJsonParser().parseResource(request.getPrefetch().getAsJsonObject("medication").getAsJsonObject("resource").toString());
            if (!bundle.hasEntry()) {
                return;
            }
            for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                this.activePrescriptions.add(getMedicationRequest(entry.getResource().getResourceType().name(), entry.getResource()));
            }
        }
    }

    private MedicationRequest getMedicationRequest(String resourceName, IBaseResource resource) throws FHIRException {
        if (resourceName.equals("MedicationOrder")) {
            MedicationOrder order = (MedicationOrder) resource;
            return Dstu2ToStu3.resolveMedicationRequest(order);
        }

        return (MedicationRequest) resource;
    }
}
