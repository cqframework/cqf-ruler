package org.opencds.cqf.ruler.cr.r4.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.devtools.DevToolsConfig;
import org.opencds.cqf.ruler.devtools.r4.CodeSystemUpdateProvider;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { MeasureEvaluateProviderIT.class,
		CrConfig.class, CqlConfig.class, DevToolsConfig.class }, properties = {
				"hapi.fhir.fhir_version=r4",
		})
public class MeasureEvaluateProviderIT extends RestIntegrationTest {

	// @Autowired
	// private MeasureEvaluateProvider measureEvaluateProvider;

	@Autowired
	private CodeSystemUpdateProvider codeSystemUpdateProvider;

	@BeforeEach
	public void setup() throws Exception {
		uploadTests("valueset");
		codeSystemUpdateProvider.updateCodeSystems();
		uploadTests("library");
	}

	@Test
	public void testMeasureEvaluate() throws Exception {
		// Patient First
		uploadTests("test/plandefinition/LungCancerScreening/Former-Smoker/Patient");
		// Map<String, IBaseResource> resources =
		// uploadTests("test/plandefinition/LungCancerScreening/Former-Smoker", ourCtx,
		// myDaoRegistry);
		// IBaseResource patient = resources.get("Former-Smoker");
		// MeasureReport report =
		// measureEvaluateProvider.evaluateMeasure(requestDetails, theId, periodStart,
		// periodEnd, reportType, subject, practitioner, lastReceivedOn, productLine);
	}
}
