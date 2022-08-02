package org.opencds.cqf.ruler.cdshooks.evaluation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.opencds.cqf.cql.engine.fhir.retrieve.Dstu3FhirQueryGenerator;
import org.opencds.cqf.cql.engine.fhir.retrieve.R4FhirQueryGenerator;
import org.opencds.cqf.cql.engine.retrieve.TerminologyAwareRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.retrieve.PriorityRetrieveProvider;
import org.opencds.cqf.ruler.cdshooks.hooks.Hook;
import org.opencds.cqf.ruler.cdshooks.providers.PrefetchDataProviderDstu2;
import org.opencds.cqf.ruler.cdshooks.providers.PrefetchDataProviderR4;
import org.opencds.cqf.ruler.cdshooks.providers.PrefetchDataProviderStu3;
import org.opencds.cqf.ruler.cdshooks.providers.ProviderConfiguration;
import org.opencds.cqf.ruler.cdshooks.exceptions.NotImplementedException;

import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.fhir.retrieve.RestFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

public abstract class EvaluationContext<T extends IBaseResource> {

    // Provided
    private final Hook hook;
    private final FhirVersionEnum fhirVersion;
    private final FhirContext fhirContext;
    private DataProvider systemProvider;
    private final Context context;
    private final T planDefinition;
    private final Library library;
    private final IGenericClient client;
    private final ProviderConfiguration providerConfiguration;
    private final ModelResolver modelResolver;

    // Requires resolution
    private DataProvider remoteProvider;
    private List<Object> contextResources;
    private List<Object> prefetchResources;

    public EvaluationContext(Hook hook, FhirVersionEnum fhirVersion, IGenericClient fhirClient, Context context,
            Library library, T planDefinition, ProviderConfiguration providerConfiguration, ModelResolver modelResolver) {
        // How to determine if it's a local server?
        // Local Server url?
        // Need a DataRetriever for that.
        // Otherwise, it's a remote data retriver.
        this.hook = hook;
        this.fhirVersion = fhirVersion;
        this.fhirContext = FhirContext.forCached(fhirVersion);
        this.context = context;
        this.planDefinition = planDefinition;
        this.library = library;
        this.client = fhirClient;
        this.providerConfiguration = providerConfiguration;
        this.modelResolver = modelResolver;
        context.registerDataProvider("http://hl7.org/fhir", getDataProvider());
    }

    public Hook getHook() {
        return hook;
    }

    public FhirVersionEnum getFhirVersion() {
        return fhirVersion;
    }

    public FhirContext getFhirContext() {
        return fhirContext;
    }

    public DataProvider getSystemProvider() {
        return systemProvider;
    }

    public T getPlanDefinition() {
        if (planDefinition == null) {
            throw new RuntimeException("Provided PlanDefinition cannot be null");
        }
        return planDefinition;
    }

    public Library getLibrary() {
        if (library == null) {
            throw new RuntimeException("Provided Library cannot be null");
        }
        return library;
    }

    private DataProvider getDataProvider() {
        if (remoteProvider == null) {
            SearchParameterResolver srp = new SearchParameterResolver(fhirContext);
            RestFhirRetrieveProvider remoteRetriever = new RestFhirRetrieveProvider(srp, modelResolver,
                getHookFhirClient());
            remoteRetriever.setTerminologyProvider(context.resolveTerminologyProvider());
            remoteRetriever.setExpandValueSets(providerConfiguration.getExpandValueSets());
            remoteRetriever.setMaxCodesPerQuery(providerConfiguration.getMaxCodesPerQuery());
            remoteRetriever.setSearchStyle(providerConfiguration.getSearchStyle());
            remoteRetriever.setModelResolver(modelResolver);
            TerminologyAwareRetrieveProvider prefetchRetriever;
            switch (fhirVersion) {
                case DSTU2:
                    // TODO: We need a dstu2 version remote data and terminology providers
                    prefetchRetriever = new PrefetchDataProviderDstu2(getPrefetchResources(), modelResolver);
                    break;
                case DSTU3:
                    prefetchRetriever = new PrefetchDataProviderStu3(getPrefetchResources(), modelResolver);
                    remoteRetriever.setFhirQueryGenerator(
                        new Dstu3FhirQueryGenerator(srp, context.resolveTerminologyProvider(), modelResolver));
                    break;
                case R4:
                    prefetchRetriever = new PrefetchDataProviderR4(getPrefetchResources(), modelResolver);
                    remoteRetriever.setFhirQueryGenerator(
                        new R4FhirQueryGenerator(srp, context.resolveTerminologyProvider(), modelResolver));
                    break;
                default:
                    throw new NotImplementedException(
                            "This CDS Hooks implementation is not configured for FHIR version: "
                                    + fhirVersion.getFhirVersionString());
            }
            // TODO: Get the "system" terminology provider.
            prefetchRetriever.setTerminologyProvider(context.resolveTerminologyProvider());
            PriorityRetrieveProvider priorityRetrieveProvider = providerConfiguration.getUseRemoteData()
                    ? new PriorityRetrieveProvider(Arrays.asList(prefetchRetriever, remoteRetriever))
                    : new PriorityRetrieveProvider(Collections.singletonList(prefetchRetriever));
            remoteProvider = new CompositeDataProvider(modelResolver, priorityRetrieveProvider);
        }
        return remoteProvider;
    }

    public Context getContext() {
        if (context == null) {
            throw new RuntimeException("The cql execution context must be provided");
        }
        return context;
    }

    public List<Object> getContextResources() {
        if (contextResources == null) {
            contextResources = EvaluationHelper.resolveContextResources(hook.getContextResources(), fhirContext);
            if (hook.getRequest().isApplyCql()) {
                contextResources = applyCqlToResources(contextResources);
            }
        }
        return contextResources;
    }

    public List<Object> getPrefetchResources() {
        if (prefetchResources == null) {
            prefetchResources = EvaluationHelper.resolvePrefetchResources(hook, fhirContext, this.getHookFhirClient(), this.providerConfiguration.getSearchStyle());
            if (hook.getRequest().isApplyCql()) {
                prefetchResources = applyCqlToResources(prefetchResources);
            }
        }
        return prefetchResources;
    }

    public IGenericClient getSystemFhirClient() {
        return this.client;
    }

    public IGenericClient getHookFhirClient() {
        IGenericClient client = this.fhirContext.newRestfulGenericClient(this.hook.getRequest().getFhirServerUrl());
        if (this.hook.getRequest().getFhirAuthorization() != null
                && hook.getRequest().getFhirAuthorization().getTokenType().equals("Bearer")) {
            BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(
                    hook.getRequest().getFhirAuthorization().getAccessToken());
            client.registerInterceptor(authInterceptor);

            // TODO: account for the expires_in, scope and subject properties within
            // workflow
        }

        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(true);
        loggingInterceptor.setLogRequestHeaders(true);
        loggingInterceptor.setLogRequestBody(true);
        loggingInterceptor.setLogResponseSummary(true);
        loggingInterceptor.setLogResponseHeaders(true);
        loggingInterceptor.setLogResponseBody(true);
        client.registerInterceptor(loggingInterceptor);

        return client;
    }

    public ProviderConfiguration getProviderConfiguration() {
        return this.providerConfiguration;
    }

    // NOTE: This is an operation defined in the cqf-ruler
    abstract List<Object> applyCqlToResources(List<Object> resources);
}
