package org.opencds.cqf.ruler.devtools.dstu3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.opencds.cqf.fhir.utility.client.Clients;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;

/**
 * This class provides an {@link OperationProvider OperationProvider}
 * implementation
 * that supports caching {@link ValueSet ValueSets } and ensuring expansion of
 * those {@link ValueSet ValueSets }
 */
public class CacheValueSetsProvider implements OperationProvider {

	@Autowired
	private IFhirSystemDao<Bundle, ?> systemDao;
	@Autowired
	private IFhirResourceDao<Endpoint> endpointDao;
	@Autowired
	private FhirContext ourCtx;

	/**
	 * Using basic authentication this {@link Operation Operation} will update
	 * any {@link ValueSet Valueset} listed given the {@link Endpoint Endpoint}
	 * provided.
	 * Any Valuesets that require expansion will be expanded.
	 *
	 * @param details    the {@link RequestDetails RequestDetails}
	 * @param endpointId the {@link Endpoint Endpoint} id
	 * @param valuesets  the {@link StringAndListParam list} of {@link ValueSet
	 *                   Valueset} ids
	 * @param userName   the userName
	 * @param password   the password
	 * @return the {@link OperationOutcome OperationOutcome} or the resulting
	 *         {@link Bundle Bundle}
	 */
	@Description(shortDefinition = "$cache-valuesets", value = "Using basic authentication this Operation will update any Valueset listed given the Endpoint provided. Any Valuesets that require expansion will be expanded.", example = "Endpoint/example-id/$cache-valuesets?valuesets=valuesetId1&valuesets=valuesetId2&user=user&password=password")
	@Operation(name = "$cache-valuesets", idempotent = true, type = Endpoint.class)
	public Resource cacheValuesets(RequestDetails details, @IdParam IdType endpointId,
			@OperationParam(name = "valuesets") StringAndListParam valuesets,
			@OperationParam(name = "user") String userName, @OperationParam(name = "pass") String password) {

		Endpoint endpoint = null;
		try {
			endpoint = this.endpointDao.read(endpointId);
			if (endpoint == null) {
				return createErrorOutcome("Could not find Endpoint/" + endpointId);
			}
		} catch (Exception e) {
			return createErrorOutcome("Could not find Endpoint/" + endpointId + "\n" + e);
		}

		IGenericClient client = Clients.forEndpoint(ourCtx, endpoint);

		if (userName != null || password != null) {
			if (userName == null) {
				return createErrorOutcome("Password was provided, but not a user name.");
			} else if (password == null) {
				return createErrorOutcome("User name was provided, but not a password.");
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
			return createErrorOutcome(e.getMessage());
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

	private OperationOutcome createErrorOutcome(String display) {
		Coding code = new Coding().setDisplay(display);
		return new OperationOutcome().addIssue(
				new OperationOutcome.OperationOutcomeIssueComponent()
						.setSeverity(OperationOutcome.IssueSeverity.ERROR)
						.setCode(OperationOutcome.IssueType.PROCESSING)
						.setDetails(new CodeableConcept().addCoding(code)));
	}
}
