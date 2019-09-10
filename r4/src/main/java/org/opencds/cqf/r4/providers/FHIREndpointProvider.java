package org.opencds.cqf.r4.providers;

import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.rp.r4.EndpointResourceProvider;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.r4.helpers.Helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FHIREndpointProvider extends EndpointResourceProvider {

    private JpaDataProvider provider;
    private IFhirSystemDao systemDao;

    public FHIREndpointProvider(JpaDataProvider provider, IFhirSystemDao systemDao) {
        this.provider = provider;
        this.systemDao = systemDao;
    }

    @Operation(name="cache-valuesets", idempotent = true)
    public Resource cacheValuesets(
            RequestDetails details,
            @IdParam IdType theId,
            @RequiredParam(name="valuesets") StringAndListParam valuesets,
            @OptionalParam(name="user") String userName,
            @OptionalParam(name="pass") String password
    ) {

        Endpoint endpoint = this.getDao().read(theId);

        if (endpoint == null) {
            return Helper.createErrorOutcome("Could not find Endpoint/" + theId);
        }

        provider.setEndpoint(endpoint.getAddress());
        provider.getFhirContext().getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        IGenericClient client = provider.getFhirClient();

        if (userName != null || password != null) {
            if (userName == null) {
                Helper.createErrorOutcome("Password was provided, but not a user name.");
            }
            else if (password == null) {
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
                            .setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.PUT).setUrl("ValueSet/" + valuesetId.getValue()))
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
        for (ValueSet.ValueSetExpansionContainsComponent expansion : expandedValueSet.getExpansion().getContains())
        {
            if (!expansion.hasSystem()) {
                continue;
            }

            if (concepts.containsKey(expansion.getSystem())) {
                concepts.get(expansion.getSystem())
                        .addConcept(
                                new ValueSet.ConceptReferenceComponent()
                                        .setCode(expansion.hasCode() ? expansion.getCode() : null)
                                        .setDisplay(expansion.hasDisplay() ? expansion.getDisplay() : null)
                        );
            }

            else {
                concepts.put(
                        expansion.getSystem(),
                        new ValueSet.ConceptSetComponent().setSystem(expansion.getSystem())
                                .addConcept(
                                        new ValueSet.ConceptReferenceComponent()
                                                .setCode(expansion.hasCode() ? expansion.getCode() : null)
                                                .setDisplay(expansion.hasDisplay() ? expansion.getDisplay() : null)
                                )
                );
            }
        }

        clean.setCompose(
                new ValueSet.ValueSetComposeComponent()
                        .setInclude(new ArrayList<>(concepts.values()))
        );

        return clean;
    }

    private ValueSet resolveValueSet(IGenericClient client, String valuesetId) {
        ValueSet valueSet = client.fetchResourceFromUrl(ValueSet.class, client.getServerBase() + "/ValueSet/" + valuesetId);

        boolean expand = false;
        if (valueSet.hasCompose()) {
            for (ValueSet.ConceptSetComponent component : valueSet.getCompose().getInclude()) {
                if (!component.hasConcept() || component.getConcept() == null) {
                    expand = true;
                }
            }
        }

        if (expand) {
            return getCachedValueSet(
                    client
                            .operation()
                            .onInstance(new IdType("ValueSet", valuesetId))
                            .named("$expand")
                            .withNoParameters(Parameters.class)
                            .returnResourceType(ValueSet.class)
                            .execute()
            );
        }

        valueSet.setVersion(valueSet.getVersion() + "-cache");
        return valueSet;
    }
}
