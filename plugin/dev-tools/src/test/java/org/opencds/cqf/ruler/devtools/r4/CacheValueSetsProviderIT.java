package org.opencds.cqf.ruler.devtools.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opencds.cqf.ruler.devtools.DevToolsConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import ca.uhn.fhir.rest.api.QualifiedParamList;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.StringAndListParam;

@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
		DevToolsConfig.class }, properties = {
				"hapi.fhir.fhir_version=r4", "hapi.fhir.cr.enabled=true" })
class CacheValueSetsProviderIT extends RestIntegrationTest {
	@Autowired
	private CacheValueSetsProvider cacheValueSetsProvider;

	@Test
	void testCacheValueSetsEndpointDNE() throws Exception {
		Endpoint endpoint = new Endpoint();
		endpoint.setId(new IdType("localhost"));
		StringAndListParam stringAndListParam = getStringAndListParamFromValueSet("valueset/AcuteInpatient.json");
		RequestDetails details = Mockito.mock(RequestDetails.class);
		Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, endpoint.getIdElement(),
				stringAndListParam, null, null);
		String detailMessage = "Could not find Endpoint/" + endpoint.getIdElement().getIdPart();
		validateOutcome(outcomeResource, detailMessage);
	}

	@Test
	void testCacheValueSetsEndpointNull() throws Exception {
		StringAndListParam stringAndListParam = getStringAndListParamFromValueSet("valueset/AcuteInpatient.json");
		RequestDetails details = Mockito.mock(RequestDetails.class);
		Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, new Endpoint().getIdElement(),
				stringAndListParam, null, null);
		validateOutcome(outcomeResource, "Could not find Endpoint/null");
	}

	@Test
	void testCacheValueSetsAuthenticationErrorUsername() throws Exception {
		Endpoint endpoint = uploadLocalServerEndpoint();
		StringAndListParam stringAndListParam = getStringAndListParamFromValueSet("valueset/AcuteInpatient.json");
		RequestDetails details = Mockito.mock(RequestDetails.class);
		Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, endpoint.getIdElement(),
				stringAndListParam, "username", null);
		String detailMessage = "User name was provided, but not a password.";
		validateOutcome(outcomeResource, detailMessage);
	}

	@Test
	void testCacheValueSetsAuthenticationErrorPassword() throws Exception {
		Endpoint endpoint = uploadLocalServerEndpoint();
		StringAndListParam stringAndListParam = getStringAndListParamFromValueSet("valueset/AcuteInpatient.json");
		RequestDetails details = Mockito.mock(RequestDetails.class);
		Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, endpoint.getIdElement(),
				stringAndListParam, null, "password");
		String detailMessage = "Password was provided, but not a user name.";
		validateOutcome(outcomeResource, detailMessage);
	}

	@Test
	void testCacheValueSetsValueSetDNE() throws Exception {
		Endpoint endpoint = uploadLocalServerEndpoint();
		StringAndListParam stringAndListParam = new StringAndListParam();
		stringAndListParam.setValuesAsQueryTokens(getFhirContext(), "valueset",
				List.of(QualifiedParamList.singleton("dne")));
		RequestDetails details = Mockito.mock(RequestDetails.class);
		Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, endpoint.getIdElement(),
				stringAndListParam, null, null);
		validateOutcome(outcomeResource, "HTTP 404 : HAPI-2001: Resource ValueSet/" + "dne" + " is not known");
	}

	@Test
	void testCacheValueSetsValueSetNull() throws Exception {
		Endpoint endpoint = uploadLocalServerEndpoint();
		StringAndListParam stringAndListParam = new StringAndListParam();
		stringAndListParam.setValuesAsQueryTokens(getFhirContext(), "valueset",
				List.of(QualifiedParamList.singleton(new ValueSet().getId())));
		RequestDetails details = Mockito.mock(RequestDetails.class);
		Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, endpoint.getIdElement(),
				stringAndListParam, null, null);
		validateOutcome(outcomeResource,
				"HTTP 404 : HAPI-2001: Resource ValueSet/" + new ValueSet().getIdElement().getIdPart() + " is not known");
	}

	@Test
	void testCacheValueSetsNoCompose() throws Exception {
		Endpoint endpoint = uploadLocalServerEndpoint();
		RequestDetails details = Mockito.mock(RequestDetails.class);
		ValueSet vs = uploadValueSet("valueset/valueset-benzodiazepine-medications.json");
		assertTrue(vs.getCompose().getInclude().isEmpty());
		StringAndListParam stringAndListParam = getStringAndListParamFromValueSet(vs);
		Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, endpoint.getIdElement(),
				stringAndListParam, null, null);
		assertTrue(outcomeResource instanceof Bundle);
		Bundle resultBundle = (Bundle) outcomeResource;
		assertEquals(1, resultBundle.getEntry().size());
		BundleEntryComponent entry = resultBundle.getEntry().get(0);
		assertTrue(entry.getResponse().getLocation().startsWith("ValueSet/" + vs.getIdElement().getIdPart()));
		assertEquals("200 OK", entry.getResponse().getStatus());
		// ValueSet resultingValueSet = createClient(ourCtx,
		// endpoint).read().resource(ValueSet.class).withId(vs.getIdElement()).execute();
		// resultingValueSet not returning with a version
		// assertTrue(resultingValueSet.getVersion().endsWith("-cached"));
	}

	// Get help with this....
	// @Test
	// void testCacheValueSetsExpandAndAddConcepts() throws Exception {
	// Endpoint endpoint = uploadLocalServerEndpoint();
	// RequestDetails details = Mockito.mock(RequestDetails.class);
	// ValueSet vs =
	// uploadValueSet("valueset/valueset-buprenorphine-and-methadone-medications.json");
	// vs.getCompose().getInclude().forEach(include -> {
	// assertTrue(!include.hasConcept());
	// });
	// StringAndListParam stringAndListParam =
	// getStringAndListParamFromValueSet(vs);

	// IGenericClient localClient = createClient(ourCtx, endpoint);
	// //
	// localClient.operation().onServer().named("updateCodeSystems").withNoParameters(Parameters.class).execute();
	// Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details,
	// endpoint.getIdElement(), stringAndListParam, null, null);
	// assertTrue(outcomeResource instanceof Bundle);
	// Bundle resultBundle = (Bundle) outcomeResource;
	// assertTrue(resultBundle.getEntry().size() == 1);
	// BundleEntryComponent entry = resultBundle.getEntry().get(0);
	// assertTrue(entry.getResponse().getLocation().startsWith("ValueSet/" +
	// vs.getIdElement().getIdPart()));
	// assertTrue(entry.getResponse().getStatus().equals("200 OK"));
	// ValueSet resultingValueSet =
	// localClient.read().resource(ValueSet.class).withId(vs.getIdElement()).execute();
	// resultingValueSet.getCompose().getInclude().forEach(include -> {
	// assertTrue(include.hasConcept());
	// });
	// }

	@Test
	void testCacheValueSetsAlreadyExpanded() throws Exception {
		Endpoint endpoint = uploadLocalServerEndpoint();
		RequestDetails details = Mockito.mock(RequestDetails.class);
		ValueSet vs = uploadValueSet("valueset/valueset-benzodiazepine-medications.json");
		StringAndListParam stringAndListParam = getStringAndListParamFromValueSet(vs);
		Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, endpoint.getIdElement(),
				stringAndListParam, null, null);
		assertTrue(outcomeResource instanceof Bundle);
		Bundle resultBundle = (Bundle) outcomeResource;
		assertEquals(1, resultBundle.getEntry().size());
		BundleEntryComponent entry = resultBundle.getEntry().get(0);
		assertTrue(entry.getResponse().getLocation().startsWith("ValueSet/" + vs.getIdElement().getIdPart()));
		assertEquals("200 OK", entry.getResponse().getStatus());
	}

	private StringAndListParam getStringAndListParamFromValueSet(String location) throws IOException {
		ValueSet vs = uploadValueSet(location);
		return getStringAndListParamFromValueSet(vs);
	}

	private StringAndListParam getStringAndListParamFromValueSet(ValueSet vs) {
		StringAndListParam stringAndListParam = new StringAndListParam();
		stringAndListParam.setValuesAsQueryTokens(getFhirContext(), "valueset",
				List.of(QualifiedParamList.singleton(vs.getIdElement().getIdPart())));
		return stringAndListParam;
	}

	private void validateOutcome(Resource outcomeResource, String detailMessage) {
		assertTrue(outcomeResource instanceof OperationOutcome);
		OperationOutcome outcome = (OperationOutcome) outcomeResource;
		for (OperationOutcomeIssueComponent issue : outcome.getIssue()) {
			assertEquals(OperationOutcome.IssueSeverity.ERROR, issue.getSeverity());
			assertTrue(issue.getDetails().getCodingFirstRep().getDisplay().startsWith(detailMessage));
		}
	}

	private ValueSet uploadValueSet(String location) throws IOException {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(CacheValueSetsProvider.class.getResourceAsStream(location))));
		String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
		reader.close();
		return (ValueSet) loadResource("json", resourceString);
	}

	private Endpoint uploadLocalServerEndpoint() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				Objects.requireNonNull(
						CacheValueSetsProvider.class.getResourceAsStream("endpoint/LocalServerEndpoint.json"))));
		String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
		reader.close();
		// Don't want to update during loading because need to setAddress first
		Endpoint endpoint = (Endpoint) loadResource("json", resourceString);
		endpoint.setAddress("http://localhost:" + getPort() + "/fhir/");
		update(endpoint);
		return endpoint;
	}
}
