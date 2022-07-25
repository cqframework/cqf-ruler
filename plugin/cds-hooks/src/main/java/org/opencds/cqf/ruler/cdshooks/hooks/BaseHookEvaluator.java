package org.opencds.cqf.ruler.cdshooks.hooks;

import java.util.List;

import org.cqframework.cql.elm.execution.ListTypeSpecifier;
import org.cqframework.cql.elm.execution.ParameterDef;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.ruler.cdshooks.evaluation.EvaluationContext;
import org.opencds.cqf.ruler.cdshooks.response.CdsCard;

import ca.uhn.fhir.rest.client.api.IGenericClient;


public abstract class BaseHookEvaluator<P extends IBaseResource> {

    protected ModelResolver modelResolver;

    protected BaseHookEvaluator(ModelResolver modelResolver) {
        this.modelResolver = modelResolver;
    }

    public List<CdsCard> evaluate(EvaluationContext<P> context) {
        // resolve context resources parameter
        // TODO - this will need some work for libraries with multiple parameters
        if (context.getLibrary().getParameters() != null && !(context.getHook() instanceof PatientViewHook)) {
            for (ParameterDef params : context.getLibrary().getParameters().getDef()) {
                if (params.getParameterTypeSpecifier() instanceof ListTypeSpecifier) {
                    context.getContext().setParameter(null, params.getName(), context.getContextResources());
                }
            }
        }
        return evaluateCdsHooksPlanDefinition(context.getContext(), context.getPlanDefinition(),
                context.getHook().getRequest().getContext().getPatientId(), context.getSystemFhirClient());
    }

    public abstract List<CdsCard> evaluateCdsHooksPlanDefinition(Context context, P planDefinition, String patientId,
            IGenericClient applyClient);
}
