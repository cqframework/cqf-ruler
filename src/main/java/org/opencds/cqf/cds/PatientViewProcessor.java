package org.opencds.cqf.cds;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.hl7.fhir.dstu3.model.PlanDefinition;

import java.util.List;

public class PatientViewProcessor extends CdsRequestProcessor {

    public PatientViewProcessor(CdsHooksRequest request, PlanDefinition planDefinition, LibraryResourceProvider libraryResourceProvider) {
        super(request, planDefinition, libraryResourceProvider);
        // TODO
    }

    @Override
    public List<CdsCard> process() {
        return null;
    }
}
