package org.opencds.cqf.ruler.security.interceptor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.devtools.DevToolsConfig;
import org.opencds.cqf.ruler.security.SecurityConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {AuthenticatorInterceptorIT.class,
	SecurityConfig.class, CrConfig.class, CqlConfig.class, DevToolsConfig.class }, properties = {
	"hapi.fhir.fhir_version=r4", "hapi.fhir.security.security_configuration.enabled=true",
	"hapi.fhir.security.security_configuration.username=someuser",
	"hapi.fhir.security.security_configuration.password=thepassword"
})
public class AuthenticatorInterceptorIT extends RestIntegrationTest {

	@Test
	public void testMeasureEvaluateAuth() throws Exception {
		String bundleAsText = stringFromResource("Exm104FhirR4MeasureBundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle)
			.withAdditionalHeader("Authorization", "Basic c29tZXVzZXI6dGhlcGFzc3dvcmQ=")
			.execute();

		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType("2019-01-01"));
		params.addParameter().setName("periodEnd").setValue(new StringType("2020-01-01"));
		params.addParameter().setName("reportType").setValue(new StringType("individual"));
		params.addParameter().setName("subject").setValue(new StringType("Patient/numer-EXM104"));
		params.addParameter().setName("lastReceivedOn").setValue(new StringType("2019-12-12"));

		MeasureReport returnMeasureReport = getClient().operation()
			.onInstance(new IdType("Measure", "measure-EXM104-8.2.000"))
			.named("$evaluate-measure")
			.withParameters(params)
			.withAdditionalHeader("Authorization", "Basic c29tZXVzZXI6dGhlcGFzc3dvcmQ=")
			.returnResourceType(MeasureReport.class)
			.execute();

		assertNotNull(returnMeasureReport);
	}

	@Test
	public void testMeasureEvaluateAuthExceptionWithoutHeader() throws Exception {
		String bundleAsText = stringFromResource("Exm104FhirR4MeasureBundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsText);

		assertThrows(AuthenticationException.class, () -> {
			getClient().transaction().withBundle(bundle).execute();
		});
	}

	@Test
	public void testMeasureEvaluateAuthExceptionWrongAuthInfo() throws Exception {
		String bundleAsText = stringFromResource("Exm104FhirR4MeasureBundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsText);

		assertThrows(AuthenticationException.class, () -> {
			getClient().transaction().withBundle(bundle)
				.withAdditionalHeader("Authorization", "Basic blahblah")
				.execute();
		});
	}
}
