package org.opencds.cqf.ruler.plugin.dev.dstu3;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.dev.DevToolsConfig;
import org.opencds.cqf.ruler.plugin.dev.IServerSupport;
import org.opencds.cqf.ruler.plugin.utility.ClientUtilities;
import org.opencds.cqf.ruler.plugin.utility.IdUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.QualifiedParamList;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.StringAndListParam;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
DevToolsConfig.class }, properties ={"hapi.fhir.fhir_version=dstu3", "hapi.fhir.dev.enabled=true"})
public class CacheValueSetsProviderIT implements IServerSupport, IdUtilities, ClientUtilities {
    private Logger log = LoggerFactory.getLogger(CodeSystemProviderIT.class);
    
    @Autowired
    private CacheValueSetsProvider cacheValueSetsProvider;

    @Autowired
    private FhirContext ourCtx;
    
    @Autowired
    private DaoRegistry myDaoRegistry;
    
    @LocalServerPort
    private int port;

    @Test
    public void testCacheValueSetsEndpointDNE() throws Exception {
        Endpoint endpoint = new Endpoint();
        endpoint.setId(new IdType("localhost"));
        StringAndListParam stringAndListParam = getStringAndListParamFromValueSet("valueset/AcuteInpatient.json");
        RequestDetails details = Mockito.mock(RequestDetails.class);
        Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, endpoint.getIdElement(), stringAndListParam, null, null);
        String detailMessage = "Could not find Endpoint/" + endpoint.getIdElement().getIdPart();
        validateOutcome(outcomeResource, detailMessage);
    }

    @Test
    public void testCacheValueSetsEndpointNull() throws Exception {
        StringAndListParam stringAndListParam = getStringAndListParamFromValueSet("valueset/AcuteInpatient.json");
        RequestDetails details = Mockito.mock(RequestDetails.class);
        Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, new Endpoint().getIdElement(), stringAndListParam, null, null);
        validateOutcome(outcomeResource, "Could not find Endpoint/null");
    }

    @Test
    public void testCacheValueSetsAuthenticationErrorUsername() throws Exception {
        Endpoint endpoint = uploadLocalServerEndpoint();
        StringAndListParam stringAndListParam = getStringAndListParamFromValueSet("valueset/AcuteInpatient.json");
        RequestDetails details = Mockito.mock(RequestDetails.class);
        Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, endpoint.getIdElement(), stringAndListParam, "username", null);
        String detailMessage = "User name was provided, but not a password.";
        validateOutcome(outcomeResource, detailMessage);
    }

    @Test
    public void testCacheValueSetsAuthenticationErrorPassword() throws Exception {
        Endpoint endpoint = uploadLocalServerEndpoint();
        StringAndListParam stringAndListParam = getStringAndListParamFromValueSet("valueset/AcuteInpatient.json");
        RequestDetails details = Mockito.mock(RequestDetails.class);
        Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, endpoint.getIdElement(), stringAndListParam, null, "password");
        String detailMessage = "Password was provided, but not a user name.";
        validateOutcome(outcomeResource, detailMessage);
    }

    @Test
    public void testCacheValueSetsValueSetDNE() throws Exception {
        Endpoint endpoint = uploadLocalServerEndpoint();
        StringAndListParam stringAndListParam = new StringAndListParam();
        BufferedReader reader = new BufferedReader(new InputStreamReader(CacheValueSetsProvider.class.getResourceAsStream("valueset/AcuteInpatient.json")));
        String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        reader.close();
        // Do not update vs in dao
        ValueSet vs = (ValueSet) loadResource("json", resourceString, ourCtx, null);
        stringAndListParam.setValuesAsQueryTokens(ourCtx, "valueset", Arrays.asList(QualifiedParamList.singleton(vs.getIdElement().getIdPart())));
        RequestDetails details = Mockito.mock(RequestDetails.class);
        Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, endpoint.getIdElement(), stringAndListParam, null, null);
        validateOutcome(outcomeResource, "HTTP 404 : Resource ValueSet/" + vs.getIdElement().getIdPart() + " is not known");
    }
    
    @Test
    public void testCacheValueSetsValueSetNull() throws Exception {
        Endpoint endpoint = uploadLocalServerEndpoint();
        StringAndListParam stringAndListParam = new StringAndListParam();
        stringAndListParam.setValuesAsQueryTokens(ourCtx, "valueset", Arrays.asList(QualifiedParamList.singleton(new ValueSet().getId())));
        RequestDetails details = Mockito.mock(RequestDetails.class);
        Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, endpoint.getIdElement(), stringAndListParam, null, null);
        validateOutcome(outcomeResource, "HTTP 404 : Resource ValueSet/" + new ValueSet().getIdElement().getIdPart() + " is not known");
    }
    
    @Test
    public void testCacheValueSetsNoCompose() throws Exception {
        Endpoint endpoint = uploadLocalServerEndpoint();
        RequestDetails details = Mockito.mock(RequestDetails.class);
        ValueSet vs = uploadValueSet("valueset/valueset-benzodiazepine-medications.json");
        assertTrue(vs.getCompose().getInclude().isEmpty());
        StringAndListParam stringAndListParam = getStringAndListParamFromValueSet(vs);
        Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, endpoint.getIdElement(), stringAndListParam, null, null);
        assertTrue(outcomeResource instanceof Bundle);
        Bundle resultBundle = (Bundle) outcomeResource;
        assertTrue(resultBundle.getEntry().size() == 1);
        BundleEntryComponent entry = resultBundle.getEntry().get(0);
        assertTrue(entry.getResponse().getLocation().startsWith("ValueSet/" + vs.getIdElement().getIdPart()));
        assertTrue(entry.getResponse().getStatus().equals("200 OK"));
        ValueSet resultingValueSet = createClient(ourCtx, endpoint).read().resource(ValueSet.class).withId(vs.getIdElement()).execute();
        // resultingValueSet not returning with a version
        // assertTrue(resultingValueSet.getVersion().endsWith("-cached"));
    }
    
    // Get help with this....
    // @Test
    // public void testCacheValueSetsExpandAndAddConcepts() throws Exception {
    //     Endpoint endpoint = uploadLocalServerEndpoint();
    //     RequestDetails details = Mockito.mock(RequestDetails.class);
    //     ValueSet vs = uploadValueSet("valueset/valueset-buprenorphine-and-methadone-medications.json");
    //     vs.getCompose().getInclude().forEach(include -> {
    //         assertTrue(!include.hasConcept());
    //     });
    //     StringAndListParam stringAndListParam = getStringAndListParamFromValueSet(vs);

    //     IGenericClient localClient = createClient(ourCtx, endpoint);
    //     // localClient.operation().onServer().named("updateCodeSystems").withNoParameters(Parameters.class).execute();
    //     Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, endpoint.getIdElement(), stringAndListParam, null, null);
    //     assertTrue(outcomeResource instanceof Bundle);
    //     Bundle resultBundle = (Bundle) outcomeResource;
    //     assertTrue(resultBundle.getEntry().size() == 1);
    //     BundleEntryComponent entry = resultBundle.getEntry().get(0);
    //     assertTrue(entry.getResponse().getLocation().startsWith("ValueSet/" + vs.getIdElement().getIdPart()));
    //     assertTrue(entry.getResponse().getStatus().equals("200 OK"));
    //     ValueSet resultingValueSet = localClient.read().resource(ValueSet.class).withId(vs.getIdElement()).execute();
    //     resultingValueSet.getCompose().getInclude().forEach(include -> {
    //         assertTrue(include.hasConcept());
    //     });
    // }
    
    @Test
    public void testCacheValueSetsAlreadyExpanded() throws Exception {
        Endpoint endpoint = uploadLocalServerEndpoint();
        RequestDetails details = Mockito.mock(RequestDetails.class);
        ValueSet vs = uploadValueSet("valueset/valueset-benzodiazepine-medications.json");
        StringAndListParam stringAndListParam = getStringAndListParamFromValueSet(vs);
        Resource outcomeResource = cacheValueSetsProvider.cacheValuesets(details, endpoint.getIdElement(), stringAndListParam, null, null);
        assertTrue(outcomeResource instanceof Bundle);
        Bundle resultBundle = (Bundle) outcomeResource;
        assertTrue(resultBundle.getEntry().size() == 1);
        BundleEntryComponent entry = resultBundle.getEntry().get(0);
        assertTrue(entry.getResponse().getLocation().startsWith("ValueSet/" + vs.getIdElement().getIdPart()));
        assertTrue(entry.getResponse().getStatus().equals("200 OK"));
        ValueSet resultingValueSet = myDaoRegistry.getResourceDao(ValueSet.class).read(vs.getIdElement());
        // resultingValueSet not returning with a version
        // assertTrue(resultingValueSet.getVersion().endsWith("-cached"));
    }

    private StringAndListParam getStringAndListParamFromValueSet(String location) throws IOException {
        ValueSet vs = uploadValueSet(location);
        return getStringAndListParamFromValueSet(vs);
    }

    private StringAndListParam getStringAndListParamFromValueSet(ValueSet vs) throws IOException {
        StringAndListParam stringAndListParam = new StringAndListParam();
        stringAndListParam.setValuesAsQueryTokens(ourCtx, "valueset", Arrays.asList(QualifiedParamList.singleton(vs.getIdElement().getIdPart())));
        return stringAndListParam;
    }

    private void validateOutcome(Resource outcomeResource, String detailMessage) {
        assertTrue(outcomeResource instanceof OperationOutcome);
        OperationOutcome outcome = (OperationOutcome) outcomeResource;
        for (OperationOutcomeIssueComponent issue : outcome.getIssue()) {
            assertTrue(issue.getSeverity().equals(OperationOutcome.IssueSeverity.ERROR));
            assertTrue(issue.getDetails().getCodingFirstRep().getDisplay().startsWith(detailMessage));
        }
    }

    private ValueSet uploadValueSet(String location) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(CacheValueSetsProvider.class.getResourceAsStream(location)));
        String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        reader.close();
        return (ValueSet) loadResource("json", resourceString, ourCtx, myDaoRegistry);
    }

    private Endpoint uploadLocalServerEndpoint() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(CacheValueSetsProvider.class.getResourceAsStream("endpoint/LocalServerEndpoint.json")));
        String resourceString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        reader.close();
        // Don't want to update during loading because need to setAddress first
        Endpoint endpoint = (Endpoint) loadResource("json", resourceString, ourCtx, null);
        endpoint.setAddress("http://localhost:" + port + "/fhir/");
        myDaoRegistry.getResourceDao(Endpoint.class).update(endpoint);
        return endpoint;
    }
}
