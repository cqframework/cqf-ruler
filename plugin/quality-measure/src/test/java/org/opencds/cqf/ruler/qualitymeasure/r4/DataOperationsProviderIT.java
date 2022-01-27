package org.opencds.cqf.ruler.qualitymeasure.r4;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.qualitymeasure.QualityMeasureConfig;
import org.opencds.cqf.ruler.test.ITestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
	QualityMeasureConfig.class }, properties = { "hapi.fhir.fhir_version=r4",
	"spring.main.allow-bean-definition-overriding=true",
	"debug=true",
	"spring.batch.job.enabled=false"})
public class DataOperationsProviderIT implements ITestSupport {
	private IGenericClient ourClient;
	private FhirContext ourCtx;

	@Autowired
	private DaoRegistry ourRegistry;

	@LocalServerPort
	private int port;

	@BeforeEach
	void beforeEach() {

		ourCtx = FhirContext.forCached(FhirVersionEnum.R4);
		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		String ourServerBase = "http://localhost:" + port + "/fhir/";
		ourClient = ourCtx.newRestfulGenericClient(ourServerBase);

	}

	@Test
	public void testR4DataRequirementsOperation() throws IOException {
		String bundleTextValueSets = stringFromResource( "LibraryTransactionBundle.json");
		FhirContext fhirContext = FhirContext.forR4();
		Bundle bundleValueSet = (Bundle)fhirContext.newJsonParser().parseResource(bundleTextValueSets);
		ourClient.transaction().withBundle(bundleValueSet).execute();

		Parameters params = new Parameters();
		params.addParameter().setName("target").setValue(new StringType("dummy"));

		Library returnLibrary = ourClient.operation().onInstance(new IdType("Library", "LibraryEvaluationTest"))
			.named("$data-requirements")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnLibrary);
	}

}
