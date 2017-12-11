package org.opencds.cqf.cds;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.dstu3.model.*;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.providers.FHIRPlanDefinitionResourceProvider;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class CdsRequestProcessor {
    CdsHooksRequest request;
    FHIRPlanDefinitionResourceProvider planDefinitionResourceProvider;
    LibraryResourceProvider libraryResourceProvider;
    DefaultProviders providers;
    boolean isStu3;

    CdsRequestProcessor(CdsHooksRequest request, LibraryResourceProvider libraryResourceProvider,
                        FHIRPlanDefinitionResourceProvider planDefinitionResourceProvider, boolean isStu3)
    {
        this.request = request;
        this.planDefinitionResourceProvider = planDefinitionResourceProvider;
        this.libraryResourceProvider = libraryResourceProvider;
        providers = new DefaultProviders(libraryResourceProvider);
        this.isStu3 = isStu3;
    }

    public abstract List<CdsCard> process();

    List<CdsCard> resolveActions(Context context) {
        return CarePlanToCdsCard.convert(planDefinitionResourceProvider.resolveCdsHooksPlanDefinition(context, request));
    }
}
