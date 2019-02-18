package org.opencds.cqf.cdshooks.hooks;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.cqframework.cql.elm.execution.ListTypeSpecifier;
import org.cqframework.cql.elm.execution.ParameterDef;
import org.opencds.cqf.cdshooks.evaluation.EvaluationContext;
import org.opencds.cqf.cdshooks.providers.PrefetchDataProviderDstu2;
import org.opencds.cqf.cdshooks.providers.PrefetchDataProviderStu3;
import org.opencds.cqf.cdshooks.response.CarePlanToCdsCard;
import org.opencds.cqf.cdshooks.response.CdsCard;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.providers.FHIRPlanDefinitionResourceProvider;

import java.io.IOException;
import java.util.List;

public class HookEvaluator {

    public static List<CdsCard> evaluate(EvaluationContext context) throws IOException {

        // resolve context resources parameter
        // TODO - this will need some work for libraries with multiple parameters
        if (context.getLibrary().getParameters() != null && !(context.getHook() instanceof PatientViewHook)) {
            for (ParameterDef params : context.getLibrary().getParameters().getDef()) {
                if (params.getParameterTypeSpecifier() instanceof ListTypeSpecifier) {
                    context.getContext().setParameter(null, params.getName(), context.getContextResources());
                }
            }
        }

        // resolve PrefetchDataProvider
        BaseFhirDataProvider prefetchDataProvider =
                context.getFhirVersion() == FhirVersionEnum.DSTU3
                        ? new PrefetchDataProviderStu3(context.getPrefetchResources())
                        : new PrefetchDataProviderDstu2(context.getPrefetchResources());
        prefetchDataProvider.setTerminologyProvider(context.getSystemProvider().getTerminologyProvider());
        context.getContext().registerDataProvider("http://hl7.org/fhir", prefetchDataProvider);
        context.getContext().registerTerminologyProvider(prefetchDataProvider.getTerminologyProvider());

        return CarePlanToCdsCard.convert(
                ((FHIRPlanDefinitionResourceProvider) context.getSystemProvider().resolveResourceProvider("PlanDefinition"))
                        .resolveCdsHooksPlanDefinition(
                                context.getContext(),
                                context.getPlanDefinition(),
                                context.getHook().getRequest().getContext().getPatientId()
                        )
        );
    }
}
