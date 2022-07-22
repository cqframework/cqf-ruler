package org.opencds.cqf.ruler.cdshooks.hooks;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.cqframework.cql.elm.execution.ListTypeSpecifier;
import org.cqframework.cql.elm.execution.ParameterDef;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.fhir.retrieve.Dstu3FhirQueryGenerator;
import org.opencds.cqf.cql.engine.fhir.retrieve.R4FhirQueryGenerator;
import org.opencds.cqf.cql.engine.fhir.retrieve.RestFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.TerminologyAwareRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.retrieve.PriorityRetrieveProvider;
import org.opencds.cqf.ruler.cdshooks.evaluation.EvaluationContext;
import org.opencds.cqf.ruler.cdshooks.providers.PrefetchDataProviderDstu2;
import org.opencds.cqf.ruler.cdshooks.providers.PrefetchDataProviderR4;
import org.opencds.cqf.ruler.cdshooks.providers.PrefetchDataProviderStu3;
import org.opencds.cqf.ruler.cdshooks.response.CdsCard;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;


public abstract class BaseHookEvaluator<P extends IBaseResource> {

    protected ModelResolver modelResolver;

    protected BaseHookEvaluator(ModelResolver modelResolver) {
        this.modelResolver = modelResolver;
    }

    public List<CdsCard> evaluate(EvaluationContext<P> context) throws IOException {

        // resolve context resources parameter
        // TODO - this will need some work for libraries with multiple parameters
        if (context.getLibrary().getParameters() != null && !(context.getHook() instanceof PatientViewHook)) {
            for (ParameterDef params : context.getLibrary().getParameters().getDef()) {
                if (params.getParameterTypeSpecifier() instanceof ListTypeSpecifier) {
                    context.getContext().setParameter(null, params.getName(), context.getContextResources());
                }
            }
        }

        SearchParameterResolver srp = new SearchParameterResolver(context.getFhirContext());
        RestFhirRetrieveProvider remoteRetriever = new RestFhirRetrieveProvider(srp, modelResolver,
          context.getHookFhirClient());

        remoteRetriever.setTerminologyProvider(context.getContext().resolveTerminologyProvider());
        remoteRetriever.setExpandValueSets(context.getProviderConfiguration().getExpandValueSets());
        remoteRetriever.setMaxCodesPerQuery(context.getProviderConfiguration().getMaxCodesPerQuery());
       remoteRetriever.setSearchStyle(context.getProviderConfiguration().getSearchStyle());
       remoteRetriever.setModelResolver(modelResolver);

        TerminologyAwareRetrieveProvider prefetchRetriever;
        if (context.getFhirVersion() == FhirVersionEnum.DSTU3) {
            prefetchRetriever = new PrefetchDataProviderStu3(context.getPrefetchResources(), modelResolver);
            remoteRetriever.setFhirQueryGenerator(
              new Dstu3FhirQueryGenerator(srp, context.getContext().resolveTerminologyProvider(), modelResolver));
        } else if (context.getFhirVersion() == FhirVersionEnum.DSTU2) {
            prefetchRetriever = new PrefetchDataProviderDstu2(context.getPrefetchResources(), modelResolver);
            // TODO: We need a dstu2 version
        }
        else {
            prefetchRetriever = new PrefetchDataProviderR4(context.getPrefetchResources(), modelResolver);
            remoteRetriever.setFhirQueryGenerator(
              new R4FhirQueryGenerator(srp, context.getContext().resolveTerminologyProvider(), modelResolver));
        }

        // TODO: Get the "system" terminology provider.
        prefetchRetriever.setTerminologyProvider(context.getContext().resolveTerminologyProvider());

        PriorityRetrieveProvider priorityRetrieveProvider = new PriorityRetrieveProvider(Arrays.asList(prefetchRetriever, remoteRetriever));
        context.getContext().registerDataProvider("http://hl7.org/fhir",
                new CompositeDataProvider(this.modelResolver, priorityRetrieveProvider));
        context.getContext().registerTerminologyProvider(prefetchRetriever.getTerminologyProvider());

        return evaluateCdsHooksPlanDefinition(context.getContext(), context.getPlanDefinition(),
                context.getHook().getRequest().getContext().getPatientId(), context.getSystemFhirClient());
    }

    public abstract List<CdsCard> evaluateCdsHooksPlanDefinition(Context context, P planDefinition, String patientId,
            IGenericClient applyClient);
}
