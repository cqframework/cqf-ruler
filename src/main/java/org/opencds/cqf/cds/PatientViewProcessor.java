package org.opencds.cqf.cds;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.cqframework.cql.elm.execution.Library;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.providers.FHIRPlanDefinitionResourceProvider;

import java.util.List;

public class PatientViewProcessor extends CdsRequestProcessor {

    public PatientViewProcessor(CdsHooksRequest request, LibraryResourceProvider libraryResourceProvider,
                                FHIRPlanDefinitionResourceProvider planDefinitionResourceProvider, boolean isStu3) {
        super(request, libraryResourceProvider, planDefinitionResourceProvider, isStu3);
    }

    @Override
    public List<CdsCard> process() {
        Context executionContext = planDefinitionResourceProvider.createExecutionContext(request);
        
        // TODO - temporary, until all unit tests and examples are modified to not expect this default library name
        if (executionContext  == null) {
            Library library = providers.getLibraryLoader().load(new org.cqframework.cql.elm.execution.VersionedIdentifier().withId("patient-view"));
            if (library != null) {
	        	executionContext = new Context(library);
	        	executionContext.registerLibraryLoader(providers.getLibraryLoader());
            }
        }

        BaseFhirDataProvider dstu3Provider = new FhirDataProviderStu3().setEndpoint(request.getFhirServer());
        // TODO - assuming terminology service is same as data provider - not a great assumption...
        dstu3Provider.setTerminologyProvider(new FhirTerminologyProvider().withEndpoint(request.getFhirServer()));
        dstu3Provider.setExpandValueSets(true);

        executionContext.registerDataProvider("http://hl7.org/fhir", dstu3Provider);
        executionContext.registerTerminologyProvider(dstu3Provider.getTerminologyProvider());
        executionContext.setContextValue("Patient", request.getContextProperty("patientId"));
        executionContext.setExpressionCaching(true);

        return resolveActions(executionContext);
    }
}
