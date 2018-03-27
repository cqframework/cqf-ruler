package org.opencds.cqf.cds;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.providers.FHIRPlanDefinitionResourceProvider;

import java.util.List;

public abstract class CdsRequestProcessor {
    CdsHooksRequest request;
    FHIRPlanDefinitionResourceProvider planDefinitionResourceProvider;
    LibraryResourceProvider libraryResourceProvider;
    DefaultProviders providers;
    boolean isStu3;
    PlanDefinition planDefinition;
    Library library;

    CdsRequestProcessor(CdsHooksRequest request, LibraryResourceProvider libraryResourceProvider,
                        FHIRPlanDefinitionResourceProvider planDefinitionResourceProvider, boolean isStu3)
    {
        this.request = request;
        this.planDefinitionResourceProvider = planDefinitionResourceProvider;
        this.libraryResourceProvider = libraryResourceProvider;
        providers = new DefaultProviders(libraryResourceProvider);
        this.isStu3 = isStu3;
        // TODO - Assuming that the first library is the primary
        this.planDefinition = planDefinitionResourceProvider.getDao().read(new IdType(request.getService()));
        if (planDefinition == null) {
            throw new IllegalArgumentException("Could not find PlanDefinition/" + request.getService());
        }
        if (planDefinition.hasLibrary()) {
            IIdType libraryId = planDefinition.getLibraryFirstRep().getReferenceElement();
            if (libraryId.hasVersionIdPart()) {
                this.library = providers.getLibraryLoader().load(
                        new VersionedIdentifier().withId(libraryId.getIdPart()).withVersion(libraryId.getVersionIdPart())
                );
            } else {
                this.library = providers.getLibraryLoader().load(new VersionedIdentifier().withId(libraryId.getIdPart()));
            }
        }
        else {
            throw new IllegalArgumentException("Missing library reference for PlanDefinition/" + request.getService());
        }
    }

    public abstract List<CdsCard> process();

    List<CdsCard> resolveActions(Context context) {
        return CarePlanToCdsCard.convert(
                planDefinitionResourceProvider.resolveCdsHooksPlanDefinition(
                        context, planDefinition, request.getContextProperty("patientId")
                )
        );
    }
}
