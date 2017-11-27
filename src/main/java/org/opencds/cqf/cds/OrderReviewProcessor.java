package org.opencds.cqf.cds;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.exceptions.MissingContextException;

import java.util.List;

public class OrderReviewProcessor extends CdsRequestProcessor {

    private ProcedureRequest contextOrder;

    public OrderReviewProcessor(CdsHooksRequest request, PlanDefinition planDefinition,
                                LibraryResourceProvider libraryResourceProvider, boolean isStu3) {
        super(request, planDefinition, libraryResourceProvider, isStu3);
        resolveOrder();
    }

    @Override
    public List<CdsCard> process() {
        // TODO - need a better way to determine library id
        Library library = providers.getLibraryLoader().load(new org.cqframework.cql.elm.execution.VersionedIdentifier().withId("OrderReview"));

        BaseFhirDataProvider dstu3Provider = new FhirDataProviderStu3().setEndpoint(request.getFhirServerEndpoint());
        // TODO - assuming terminology service is same as data provider - not a great assumption...
        dstu3Provider.setTerminologyProvider(new FhirTerminologyProvider().withEndpoint(request.getFhirServerEndpoint()));
        dstu3Provider.setExpandValueSets(true);

        Context executionContext = new Context(library);
        executionContext.registerLibraryLoader(providers.getLibraryLoader());
        executionContext.registerDataProvider("http://hl7.org/fhir", dstu3Provider);
        executionContext.registerTerminologyProvider(dstu3Provider.getTerminologyProvider());
        executionContext.setContextValue("Patient", request.getPatientId());
        executionContext.setParameter(null, "Order", contextOrder);
        executionContext.setExpressionCaching(true);

        return resolveActions(executionContext);
    }

    private void resolveOrder() {
        if (request.getContext().size() == 0) {
            throw new MissingContextException("The order-review request requires the context to contain an order.");
        }

        // Assuming STU3 here as per the example here: http://cds-hooks.org/#radiology-appropriateness
        this.contextOrder = (ProcedureRequest) FhirContext.forDstu3().newJsonParser().parseResource(request.getContext().get(0).toString());
    }
}
