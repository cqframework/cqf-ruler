package org.opencds.cqf.ruler.ra.r4;

import ca.uhn.fhir.context.FhirContext;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
	AssistedProviderIT.class }, properties = { "hapi.fhir.fhir_version=r4" })
class AssistedProviderIT extends RestIntegrationTest {
	private String serverBase;

	@BeforeEach
	void beforeEach() {
		serverBase = "http://localhost:" + getPort() + "/assisted";
	}

	@Test
	void testAssistedServerStringRequest() {
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpPost request = new HttpPost(serverBase);
			request.setEntity(new StringEntity("periodStart,periodEnd,modelId,modelVersion,patientId,ccCode,suspectType,evidenceStatus,evidenceStatusDate,hiearchicalStatus\n" +
				"2021-01-01,2021-09-30,https://build.fhir.org/ig/HL7/davinci-ra/Measure-RAModelExample01,24,ra-patient01,18,historic,closed-gap,2021-04-01,applied-not-superseded\n" +
				"2021-01-01,2021-09-30,https://build.fhir.org/ig/HL7/davinci-ra/Measure-RAModelExample01,24,ra-patient01,111,historic,pending,2021-09-29,applied-not-superseded\n" +
				"2021-01-01,2021-09-30,https://build.fhir.org/ig/HL7/davinci-ra/Measure-RAModelExample01,24,ra-patient01,24,historic,open-gap,2020-07-15,applied-not-superseded\n" +
				"2021-01-01,2021-09-30,https://build.fhir.org/ig/HL7/davinci-ra/Measure-RAModelExample01,24,ra-patient01,112,historic,closed-gap,2021-04-27,applied-superseded\n" +
				"2021-01-01,2021-09-30,https://build.fhir.org/ig/HL7/davinci-ra/Measure-RAModelExample01,24,ra-patient01,19,historic,pending,2021-09-27,applied-superseded\n" +
				"2021-01-01,2021-09-30,https://build.fhir.org/ig/HL7/davinci-ra/Measure-RAModelExample01,24,ra-patient01,84,historic,open-gap,2020-12-15,applied-superseded\n" +
				"2021-01-01,2021-09-30,https://build.fhir.org/ig/HL7/davinci-ra/Measure-RAModelExample01,24,ra-patient01,22,suspected,closed-gap,2021-03-15,applied-not-superseded\n" +
				"2021-01-01,2021-09-30,https://build.fhir.org/ig/HL7/davinci-ra/Measure-RAModelExample01,24,ra-patient01,96,suspected,pending,2021-09-27,applied-not-superseded\n" +
				"2021-01-01,2021-09-30,https://build.fhir.org/ig/HL7/davinci-ra/Measure-RAModelExample01,24,ra-patient01,110,suspected,open-gap,2020-07-15,applied-not-superseded\n" +
				"2021-01-01,2021-09-30,https://build.fhir.org/ig/HL7/davinci-ra/Measure-RAModelExample01,24,ra-patient01,83,net-new,pending,2021-09-28,applied-not-superseded\n" +
				"2021-01-01,2021-09-30,https://build.fhir.org/ig/HL7/davinci-ra/Measure-RAModelExample01,24,ra-patient01,59,historic,open-gap,2020-07-15,applied-not-superseded"));
			request.addHeader("Content-Type", "text/csv");

			CloseableHttpResponse response = httpClient.execute(request);
			String result = EntityUtils.toString(response.getEntity());
			validateResult(result);
		} catch (IOException ioe) {
			fail(ioe.getMessage());
		}
	}

	@Test
	void testAssistedServerFileRequest() {
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpPost request = new HttpPost(serverBase);
			request.setEntity(new FileEntity(new File(Objects.requireNonNull(this.getClass().getResource("test.csv")).toURI())));
			request.addHeader("Content-Type", "text/csv");

			CloseableHttpResponse response = httpClient.execute(request);
			String result = EntityUtils.toString(response.getEntity());
			validateResult(result);
		} catch (IOException ioe) {
			fail(ioe.getMessage());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	void validateResult(String result) {
		IBaseResource resource = FhirContext.forR4Cached().newJsonParser().parseResource(result);
		assertTrue(resource instanceof Bundle);
		Bundle bundle = (Bundle) resource;
		assertTrue(bundle.hasType());
		assertEquals(Bundle.BundleType.TRANSACTION, bundle.getType());
		assertTrue(bundle.hasEntry());
		assertTrue(bundle.getEntryFirstRep().hasResource());
		assertTrue(bundle.getEntryFirstRep().getResource() instanceof MeasureReport);
		MeasureReport mr = (MeasureReport) bundle.getEntryFirstRep().getResource();
		assertTrue(mr.hasStatus() && mr.getStatus() == MeasureReport.MeasureReportStatus.COMPLETE);
		assertTrue(mr.hasType() && mr.getType() == MeasureReport.MeasureReportType.INDIVIDUAL);
		assertTrue(mr.hasPeriod());
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		assertEquals("2021-01-01", formatter.format(mr.getPeriod().getStart()));
		assertEquals("2021-09-30", formatter.format(mr.getPeriod().getEnd()));
		assertTrue(mr.hasGroup());
		assertEquals(11, mr.getGroup().size());
	}
}
