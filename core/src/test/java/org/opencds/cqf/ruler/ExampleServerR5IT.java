package org.opencds.cqf.ruler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class, properties = {
		"spring.batch.job.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:dbr5",
		"hapi.fhir.fhir_version=r5",
		"hapi.fhir.subscription.websocket_enabled=true",
		"hapi.fhir.mdm_enabled=false"
})
public class ExampleServerR5IT {

	// private static final org.slf4j.Logger ourLog =
	// org.slf4j.LoggerFactory.getLogger(ExampleServerR5IT.class);
	private IGenericClient ourClient;
	private FhirContext ourCtx;

	@LocalServerPort
	private int port;

	@Test
	public void testCreateAndRead() {

		String methodName = "testCreateResourceConditional";

		Patient pt = new Patient();
		pt.addName().setFamily(methodName);
		IIdType id = ourClient.create().resource(pt).execute().getId();

		Patient pt2 = ourClient.read().resource(Patient.class).withId(id).execute();
		assertEquals(methodName, pt2.getName().get(0).getFamily());
	}

	// @Ignore("not yet ready")
	// @Test
	// public void testWebsocketSubscription() throws Exception {

	// /*
	// * Create topic
	// */
	// SubscriptionTopic topic = new SubscriptionTopic();
	// topic.addResourceTrigger().getQueryCriteria().setCurrent("Observation?status=final");

	// /*
	// * Create subscription
	// */
	// Subscription subscription = new Subscription();
	// subscription.addFilterBy().setValue("Observation?status=final");
	// subscription.setTopic("Observation?status=final");
	// subscription.setReason("Monitor new neonatal function (note, age will be
	// determined by the monitor)");
	// subscription.setStatus(Enumerations.SubscriptionState.REQUESTED);
	// subscription.getChannelType()
	// .setSystem("http://terminology.hl7.org/CodeSystem/subscription-channel-type")
	// .setCode("websocket");
	// subscription.setContentType("application/json");

	// MethodOutcome methodOutcome =
	// ourClient.create().resource(subscription).execute();
	// IIdType mySubscriptionId = methodOutcome.getId();

	// // Wait for the subscription to be activated
	// waitForSize(1, () ->
	// ourClient.search().forResource(Subscription.class).where(Subscription.STATUS.exactly().code("active")).cacheControl(new
	// CacheControlDirective().setNoCache(true)).returnBundle(Bundle.class).execute().getEntry().size());

	// /*
	// * Attach websocket
	// */

	// WebSocketClient myWebSocketClient = new WebSocketClient();
	// SocketImplementation mySocketImplementation = new
	// SocketImplementation(mySubscriptionId.getIdPart(), EncodingEnum.JSON);

	// myWebSocketClient.start();
	// URI echoUri = new URI("ws://localhost:" + port + "/websocket");
	// ClientUpgradeRequest request = new ClientUpgradeRequest();
	// ourLog.info("Connecting to : {}", echoUri);
	// Future<Session> connection =
	// myWebSocketClient.connect(mySocketImplementation, echoUri, request);
	// Session session = connection.get(2, TimeUnit.SECONDS);

	// ourLog.info("Connected to WS: {}", session.isOpen());

	// /*
	// * Create a matching resource
	// */
	// Observation obs = new Observation();
	// obs.setStatus(Enumerations.ObservationStatus.FINAL);
	// ourClient.create().resource(obs).execute();

	// /*
	// * Ensure that we receive a ping on the websocket
	// */
	// await().until(() -> mySocketImplementation.myPingCount > 0);

	// /*
	// * Clean up
	// */
	// ourClient.delete().resourceById(mySubscriptionId).execute();
	// }

	@BeforeEach
	void beforeEach() {

		ourCtx = FhirContext.forCached(FhirVersionEnum.R5);
		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		String ourServerBase = "http://localhost:" + port + "/fhir/";
		ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
		ourClient.registerInterceptor(new LoggingInterceptor(true));
	}

	@BeforeAll
	public static void setup() {
		Awaitility.setDefaultTimeout(30, TimeUnit.SECONDS);
	}
}
