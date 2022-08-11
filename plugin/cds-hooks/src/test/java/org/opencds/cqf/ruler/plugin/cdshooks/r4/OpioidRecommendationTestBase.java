package org.opencds.cqf.ruler.plugin.cdshooks.r4;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.cdshooks.CdsHooksConfig;
import org.opencds.cqf.ruler.cdshooks.CdsServicesCache;
import org.opencds.cqf.ruler.plugin.cdshooks.ResourceChangeEvent;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { Application.class, CdsHooksConfig.class },
        properties = {"hapi.fhir.fhir_version=r4", "hapi.fhir.security.basic_auth.enabled=false"})
public abstract class OpioidRecommendationTestBase extends RestIntegrationTest {

    @Autowired
    CdsServicesCache cdsServicesCache;
    private String ourCdsBase;

    @BeforeEach
    void beforeEach() {
        ourCdsBase = "http://localhost:" + getPort() + "/cds-services";
    }

    public void makeRequest(String planDefinitionId, String requestFileName) {
        ResourceChangeEvent rce = new ResourceChangeEvent();
        rce.setCreatedResourceIds(
                Collections.singletonList(new IdType("PlanDefinition/" + planDefinitionId)));
        cdsServicesCache.handleChange(rce);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String cdsHooksRequestString = stringFromResource(requestFileName);
            Gson jsonParser = new Gson();
            JsonObject cdsHooksRequestObject = jsonParser.fromJson(cdsHooksRequestString, JsonObject.class);
            cdsHooksRequestObject.addProperty("fhirServer", getServerBase());

            HttpPost request = new HttpPost(ourCdsBase + "/" + planDefinitionId);
            request.setEntity(new StringEntity(cdsHooksRequestObject.toString()));
            request.addHeader("Content-Type", "application/json");

            CloseableHttpResponse response = httpClient.execute(request);
            validate(EntityUtils.toString(response.getEntity()));
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
    }

    abstract void validate(String response);
}
