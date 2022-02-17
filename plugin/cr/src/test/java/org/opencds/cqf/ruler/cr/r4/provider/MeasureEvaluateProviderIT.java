package org.opencds.cqf.ruler.cr.r4.provider;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.devtools.DevToolsConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;


import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = { MeasureEvaluateProviderIT.class,
		CrConfig.class, CqlConfig.class, DevToolsConfig.class }, properties = {
				"hapi.fhir.fhir_version=r4"
		})
public class MeasureEvaluateProviderIT extends RestIntegrationTest {


	@Test
	public void testMeasureEvaluate() throws Exception {
		String bundleAsText = stringFromResource( "Exm104FhirR4MeasureBundle.json");
		Bundle bundle = (Bundle)getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType("2019-01-01"));
		params.addParameter().setName("periodEnd").setValue(new StringType("2020-01-01"));
		params.addParameter().setName("reportType").setValue(new StringType("individual"));
		params.addParameter().setName("subject").setValue(new StringType("Patient/numer-EXM104"));
		params.addParameter().setName("lastReceivedOn").setValue(new StringType("2019-12-12"));

		MeasureReport  returnMeasureReport = getClient().operation().onInstance(new IdType("Measure", "measure-EXM104-8.2.000"))
			.named("$evaluate-measure")
			.withParameters(params)
			.returnResourceType(MeasureReport.class)
			.execute();

		assertNotNull(returnMeasureReport);
		//System.out.println("Resource:"+this.getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(returnMeasureReport));
	}

	@Test
	public void testMeasureEvaluateWithAdditionalData() throws Exception {
		String mainBundleAsText = stringFromResource( "Exm104MeasurePartBundle.json");
		Bundle bundle = (Bundle)getFhirContext().newJsonParser().parseResource(mainBundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		String additionalBundleAsText = stringFromResource( "Exm104MeasureAdditionalData.json");
		Bundle additionalData = (Bundle)getFhirContext().newJsonParser().parseResource(additionalBundleAsText);


		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType("2019-01-01"));
		params.addParameter().setName("periodEnd").setValue(new StringType("2020-01-01"));
		params.addParameter().setName("reportType").setValue(new StringType("subject"));
		params.addParameter().setName("subject").setValue(new StringType("Patient/numer-EXM104"));
		params.addParameter().setName("lastReceivedOn").setValue(new StringType("2019-12-12"));
		params.addParameter().setName("additionalData").setResource(additionalData);

		MeasureReport  returnMeasureReport = getClient().operation().onInstance(new IdType("Measure", "measure-EXM104-8.2.000"))
			.named("$evaluate-measure")
			.withParameters(params)
			.returnResourceType(MeasureReport.class)
			.execute();

		assertNotNull(returnMeasureReport);
	}

}
