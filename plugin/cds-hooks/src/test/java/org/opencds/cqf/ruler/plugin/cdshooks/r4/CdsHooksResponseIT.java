package org.opencds.cqf.ruler.plugin.cdshooks.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Collections;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.cdshooks.CdsHooksConfig;
import org.opencds.cqf.ruler.cdshooks.CdsServicesCache;
import org.opencds.cqf.ruler.plugin.cdshooks.ResourceChangeEvent;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CdsHooksResponseIT.class,
		CdsHooksConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
class CdsHooksResponseIT extends RestIntegrationTest {
	@Autowired
	CdsServicesCache cdsServicesCache;
	private String ourCdsBase;

	@BeforeEach
	void beforeEach() {
		ourCdsBase = "http://localhost:" + getPort() + "/cds-services";
	}

	@Test
	void testUnicodeResponse() {
		loadResource("library-unicode.json");
		loadResource("plandefinition-unicode.json");

		ResourceChangeEvent rce = new ResourceChangeEvent();
		rce.setUpdatedResourceIds(Collections.singletonList(new IdType("unicode")));
		cdsServicesCache.handleChange(rce);

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			String cdsHooksRequestString = stringFromResource("unicode-request.json");
			Gson jsonParser = new Gson();
			JsonObject cdsHooksRequestObject = jsonParser.fromJson(cdsHooksRequestString, JsonObject.class);
			cdsHooksRequestObject.addProperty("fhirServer", getServerBase());

			HttpPost request = new HttpPost(ourCdsBase + "/unicode");
			request.setEntity(new StringEntity(cdsHooksRequestObject.toString()));
			request.addHeader("Content-Type", "application/json");

			CloseableHttpResponse response = httpClient.execute(request);
			String result = EntityUtils.toString(response.getEntity());

			String expected = "{\n" +
					"  \"cards\": [\n" +
					"    {\n" +
					"      \"summary\": \"This character is not handled: ≥\",\n" +
					"      \"detail\": \"None\",\n" +
					"      \"indicator\": \"info\",\n" +
					"      \"links\": []\n" +
					"    }\n" +
					"  ]\n" +
					"}\n";

			assertEquals(expected, result);
		} catch (IOException ioe) {
			fail(ioe.getMessage());
		}
	}

	@Test
	void testHtmlResponse() {
		loadResource("library-html.json");
		loadResource("plandefinition-html.json");

		ResourceChangeEvent rce = new ResourceChangeEvent();
		rce.setUpdatedResourceIds(Collections.singletonList(new IdType("html")));
		cdsServicesCache.handleChange(rce);

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			String cdsHooksRequestString = stringFromResource("html-request.json");
			Gson jsonParser = new Gson();
			JsonObject cdsHooksRequestObject = jsonParser.fromJson(cdsHooksRequestString, JsonObject.class);
			cdsHooksRequestObject.addProperty("fhirServer", getServerBase());

			HttpPost request = new HttpPost(ourCdsBase + "/html");
			request.setEntity(new StringEntity(cdsHooksRequestObject.toString()));
			request.addHeader("Content-Type", "application/json");

			CloseableHttpResponse response = httpClient.execute(request);
			String result = EntityUtils.toString(response.getEntity());

			String expected = "{\n" +
					"  \"cards\": [\n" +
					"    {\n" +
					"      \"summary\": \"This character is not handled: <br />\",\n" +
					"      \"detail\": \"None\",\n" +
					"      \"indicator\": \"info\",\n" +
					"      \"links\": []\n" +
					"    }\n" +
					"  ]\n" +
					"}\n";

			assertEquals(expected, result);
		} catch (IOException ioe) {
			fail(ioe.getMessage());
		}
	}
}
