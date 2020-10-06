package org.opencds.cqf.dstu3.providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.opencds.cqf.dstu3.helpers.Helper;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;

@Component
public class CacheValueSetsProvider {

    private IFhirSystemDao<Bundle, ?> systemDao;
    private IFhirResourceDao<Endpoint> endpointDao;

    @Inject
    public CacheValueSetsProvider(IFhirSystemDao<Bundle, ?> systemDao, IFhirResourceDao<Endpoint> endpointDao) {
        this.systemDao = systemDao;
        this.endpointDao = endpointDao;
    }

    @Operation(name = "cache-valuesets", idempotent = true, type = Endpoint.class)
    public Resource cacheValuesets(RequestDetails details, @IdParam IdType theId,
            @OperationParam(name = "valuesets") StringAndListParam valuesets,
            @OperationParam(name = "user") String userName, @OperationParam(name = "pass") String password) {

        Endpoint endpoint = this.endpointDao.read(theId);

        if (endpoint == null) {
            return Helper.createErrorOutcome("Could not find Endpoint/" + theId);
        }

        IGenericClient client = this.systemDao.getContext().newRestfulGenericClient(endpoint.getAddress());

        if (userName != null || password != null) {
            if (userName == null) {
                Helper.createErrorOutcome("Password was provided, but not a user name.");
            } else if (password == null) {
                Helper.createErrorOutcome("User name was provided, but not a password.");
            }

            BasicAuthInterceptor basicAuth = new BasicAuthInterceptor(userName, password);
            client.registerInterceptor(basicAuth);

            // TODO - more advanced security like bearer tokens, etc...
        }

        try {
            Bundle bundleToPost = new Bundle();
            for (StringOrListParam params : valuesets.getValuesAsQueryTokens()) {
                for (StringParam valuesetId : params.getValuesAsQueryTokens()) {
                    bundleToPost.addEntry()
                            .setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.PUT)
                                    .setUrl("ValueSet/" + valuesetId.getValue()))
                            .setResource(resolveValueSet(client, valuesetId.getValue()));
                }
            }

            return (Resource) systemDao.transaction(details, bundleToPost);
        } catch (Exception e) {
            return Helper.createErrorOutcome(e.getMessage());
        }
    }

    private ValueSet getCachedValueSet(ValueSet expandedValueSet) {
        ValueSet clean = expandedValueSet.copy().setExpansion(null);

        Map<String, ValueSet.ConceptSetComponent> concepts = new HashMap<>();
        for (ValueSet.ValueSetExpansionContainsComponent expansion : expandedValueSet.getExpansion().getContains()) {
            if (!expansion.hasSystem()) {
                continue;
            }

            if (concepts.containsKey(expansion.getSystem())) {
                concepts.get(expansion.getSystem())
                        .addConcept(new ValueSet.ConceptReferenceComponent()
                                .setCode(expansion.hasCode() ? expansion.getCode() : null)
                                .setDisplay(expansion.hasDisplay() ? expansion.getDisplay() : null));
            }

            else {
                concepts.put(expansion.getSystem(),
                        new ValueSet.ConceptSetComponent().setSystem(expansion.getSystem())
                                .addConcept(new ValueSet.ConceptReferenceComponent()
                                        .setCode(expansion.hasCode() ? expansion.getCode() : null)
                                        .setDisplay(expansion.hasDisplay() ? expansion.getDisplay() : null)));
            }
        }

        clean.setCompose(new ValueSet.ValueSetComposeComponent().setInclude(new ArrayList<>(concepts.values())));

        return clean;
    }

    private ValueSet resolveValueSet(IGenericClient client, String valuesetId) {
        ValueSet valueSet = client.fetchResourceFromUrl(ValueSet.class,
                client.getServerBase() + "/ValueSet/" + valuesetId);

        boolean expand = false;
        if (valueSet.hasCompose()) {
            for (ValueSet.ConceptSetComponent component : valueSet.getCompose().getInclude()) {
                if (!component.hasConcept() || component.getConcept() == null) {
                    expand = true;
                }
            }
        }

        if (expand) {
            return getCachedValueSet(client.operation().onInstance(new IdType("ValueSet", valuesetId)).named("$expand")
                    .withNoParameters(Parameters.class).returnResourceType(ValueSet.class).execute());
        }

        valueSet.setVersion(valueSet.getVersion() + "-cache");
        return valueSet;
    }
}
