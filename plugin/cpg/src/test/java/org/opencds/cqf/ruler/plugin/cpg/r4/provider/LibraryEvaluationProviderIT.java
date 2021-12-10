package org.opencds.cqf.ruler.plugin.cpg.r4.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.plugin.cpg.CpgConfig;
import org.opencds.cqf.ruler.plugin.cpg.CpgProperties;
import org.opencds.cqf.ruler.plugin.testutility.ResolutionUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
        CpgConfig.class }, properties = { "hapi.fhir.fhir_version=r4", "hapi.fhir.cpg.enabled=true",
														"spring.main.allow-bean-definition-overriding=true",
														"debug=true",
														"spring.batch.job.enabled=false",
														"hapi.fhir.cql_enabled=true" })
public class LibraryEvaluationProviderIT implements ResolutionUtilities {
	private IGenericClient ourClient;
	private FhirContext ourCtx;

	@Autowired
	private DaoRegistry ourRegistry;

	@Autowired
	private CpgProperties cpgProperties;

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
	public void testLibraryExecutionProvider() throws Exception {
		System.out.println("Made it here.");
	}

	@Test
	public void testLibraryEvaluationValidationThrows() throws IOException {

		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType("2021-01-01"));
		params.addParameter().setName("periodEnd").setValue(new StringType("2021-12-31"));
		params.addParameter().setName("patientId").setValue(null);
		params.addParameter().setName("context").setValue(new StringType("Patient"));

		String packagePrefix = "org.opencds.cqf.ruler.plugin.cpg.r4.provider/";
		resolveByLocation(ourRegistry, packagePrefix + "ColorectalCancerScreeningsFHIR.json", ourCtx);
		Library lib = ourClient.read().resource(Library.class).withId("ColorectalCancerScreeningsFHIR").execute();
		assertNotNull(lib);

		assertThrows(InternalErrorException.class, () -> {
			ourClient.operation().onInstance(new IdType("Library", "ColorectalCancerScreeningsFHIR"))
				.named("$evaluate")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
		});
	}

	@Test
	public void testLibraryEvaluationValidData() throws IOException {

		String packagePrefix = "org.opencds.cqf.ruler.plugin.cpg.r4.provider/";
		resolveByLocation(ourRegistry, packagePrefix + "ColorectalCancerScreeningsFHIR.json", ourCtx);

		String bundleTextValueSets = stringFromResource(packagePrefix + "valuesets-ColorectalCancerScreeningsFHIR-bundle.json");
		FhirContext fhirContext = FhirContext.forR4();
		Bundle bundleValueSet = (Bundle)fhirContext.newJsonParser().parseResource(bundleTextValueSets);
		Bundle resultValueSet = ourClient.transaction().withBundle(bundleValueSet).execute();

		String bundleText = stringFromResource(packagePrefix + "additionalData.json");
		Bundle bundle = (Bundle)fhirContext.newJsonParser().parseResource(bundleText);
		Library lib = ourClient.read().resource(Library.class).withId("ColorectalCancerScreeningsFHIR").execute();

		assertNotNull(bundle);
		assertNotNull(lib);

		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType("2019-01-01"));
		params.addParameter().setName("periodEnd").setValue(new StringType("2019-12-31"));
		params.addParameter().setName("patientId").setValue(new StringType("numer-EXM130"));
		params.addParameter().setName("context").setValue(new StringType("Patient"));
		params.addParameter().setName("additionalData").setResource(bundle);


		Bundle returnBundle = ourClient.operation().onInstance(new IdType("Library", "ColorectalCancerScreeningsFHIR"))
			.named("$evaluate")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();

		System.out.println(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(returnBundle));
		assertNotNull(returnBundle);
	}

}
