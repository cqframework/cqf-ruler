package org.opencds.cqf.ruler.casereporting.r4;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.casereporting.CaseReportingConfig;
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
	CaseReportingConfig.class }, properties = { "hapi.fhir.fhir_version=r4",
	"spring.main.allow-bean-definition-overriding=true",
	"debug=true",
	"spring.batch.job.enabled=false"})
public class MeasureDataProcessProviderIT implements ITestSupport {
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
	public void testMeasureReportExtractLineListData() throws IOException {

		String packagePrefix = "org/opencds/cqf/ruler/casereporting/r4/";
		loadResource(packagePrefix + "Patient-ra-patient01.json", ourCtx, ourRegistry);
		loadResource(packagePrefix + "Patient-ra-patient02.json", ourCtx, ourRegistry);
		loadResource(packagePrefix + "Patient-ra-patient03.json", ourCtx, ourRegistry);
		loadResource( packagePrefix + "Group-ra-group00.json", ourCtx, ourRegistry);
		loadResource( packagePrefix + "Group-ra-group01.json", ourCtx, ourRegistry);
		loadResource( packagePrefix + "Group-ra-group02.json", ourCtx, ourRegistry);
		loadResource(  packagePrefix + "MeasureReport-ra-measurereport01.json", ourCtx, ourRegistry);

		MeasureReport measureReport = ourClient.read().resource(MeasureReport.class).withId("ra-measurereport01").execute();

		assertNotNull(measureReport);

		Parameters params = new Parameters();
		params.addParameter().setName("measureReport").setResource(measureReport);
		params.addParameter().setName("subjectList").setValue(null);


		Bundle returnBundle = ourClient.operation().onType(MeasureReport.class)
			.named("$extract-line-list-data")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();

		assertNotNull(returnBundle);
	}

}
