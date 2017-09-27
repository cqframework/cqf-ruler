package org.opencds.cqf.cds;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import com.google.gson.JsonObject;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.opencds.cqf.cql.execution.Context;

import java.util.List;

public class OrderReviewProcessor extends CdsRequestProcessor {

    public OrderReviewProcessor(CdsHooksRequest request, PlanDefinition planDefinition, LibraryResourceProvider libraryResourceProvider) {
        super(request, planDefinition, libraryResourceProvider);
        // TODO
    }

    @Override
    public List<CdsCard> process() {
        return null;
    }
}
