package org.opencds.cqf.cds;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.Resource;
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

    List<MedicationRequest> contextPrescriptions;
    List<MedicationRequest> activePrescriptions;

    public MedicationPrescribeProcessor(CdsHooksRequest request, LibraryResourceProvider libraryResourceProvider,
                                        FHIRPlanDefinitionResourceProvider planDefinitionResourceProvider, boolean isStu3)
            throws FHIRException
    {
        super(request, libraryResourceProvider, planDefinitionResourceProvider, isStu3);

        this.contextPrescriptions = new ArrayList<>();
        this.activePrescriptions = new ArrayList<>();
        resolveContextPrescription();
        resolveActivePrescriptions();
    }

    @Override
    public List<CdsCard> process() {
        BaseFhirDataProvider dstu3Provider = new FhirDataProviderStu3().setEndpoint(request.getFhirServer());
        // TODO - assuming terminology service is same as data provider - not a great assumption...
        dstu3Provider.setTerminologyProvider(new FhirTerminologyProvider().withEndpoint(request.getFhirServer()));
        dstu3Provider.setExpandValueSets(true);

        Context executionContext = new Context(library);
        executionContext.registerLibraryLoader(providers.getLibraryLoader());
        executionContext.registerDataProvider("http://hl7.org/fhir", dstu3Provider);
        executionContext.setExpressionCaching(true);
        executionContext.setParameter(null, "Orders", activePrescriptions);

        return resolveActions(executionContext);
    }

    private void resolveContextPrescription() throws FHIRException {
        for (org.hl7.fhir.dstu3.model.Resource resource : request.getContextResources("medications")) {
            this.contextPrescriptions.add((MedicationRequest) resource);
        }
    }

    private void resolveActivePrescriptions() throws FHIRException {
        this.activePrescriptions.addAll(this.contextPrescriptions); // include the context prescription

        if (!isStu3) {
            IBaseResource resource = FhirContext.forDstu2Hl7Org().newJsonParser().parseResource(request.getPrefetch().getAsJsonObject("medications").getAsJsonObject("resource").toString());
            if (resource instanceof org.hl7.fhir.instance.model.Bundle) {
                org.hl7.fhir.instance.model.Bundle bundle = (org.hl7.fhir.instance.model.Bundle) resource;
                if (bundle.getEntry() == null) {
                    return;
                }
                for (org.hl7.fhir.instance.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                    this.activePrescriptions.add(getMedicationRequest(entry.getResource()));
                }
            }
        }
        else {
            org.hl7.fhir.dstu3.model.Bundle bundle = (org.hl7.fhir.dstu3.model.Bundle) FhirContext.forDstu3().newJsonParser().parseResource(request.getPrefetch().getAsJsonObject("medications").getAsJsonObject("resource").toString());
            if (!bundle.hasEntry()) {
                return;
            }
            for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                this.activePrescriptions.add(getMedicationRequest(entry.getResource()));
            }
        }
    }

    private MedicationRequest getMedicationRequest(IBaseResource resource) throws FHIRException {
        if (resource.getIdElement().getResourceType().equals("MedicationOrder")) {
            return (MedicationRequest) Dstu2ToStu3.convertResource((Resource) resource);
        }

        return (MedicationRequest) resource;
    }
}
