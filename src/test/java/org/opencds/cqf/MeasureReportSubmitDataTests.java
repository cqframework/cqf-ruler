package org.opencds.cqf;

import ca.uhn.fhir.model.primitive.IdDt;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Parameters;
import org.junit.Assert;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

class MeasureReportSubmitDataTests {

    private TestServer server;
    private final String measureReportLocation = "measure-report-submit-data/";

    MeasureReportSubmitDataTests(TestServer server) {
        this.server = server;
    }

    // TODO - Need a test that is expected to fail and more stringent tests
    void submitDataTest_NonTransaction() {
        runSubmitDataOperation("asf-submit-data-bundle.json");
    }

    void submitDataTest_Transaction() {
        runSubmitDataOperation("asf-submit-data-transaction-bundle.json");
    }

    private void runSubmitDataOperation(String bundleName) {
        InputStream is = MeasureReportSubmitDataTests.class.getResourceAsStream(measureReportLocation + "asf-measure-report.json");
        MeasureReport report = (MeasureReport) server.dataProvider.getFhirContext().newJsonParser().parseResource(new InputStreamReader(is));
        is = MeasureReportSubmitDataTests.class.getResourceAsStream(measureReportLocation + bundleName);
        Bundle bundle = (Bundle) server.dataProvider.getFhirContext().newJsonParser().parseResource(new InputStreamReader(is));

        Parameters inParams = new Parameters();
        inParams.addParameter().setName("measure-report").setResource(report);
        inParams.addParameter().setName("resource").setResource(bundle);

        Parameters outParams = server.ourClient
                .operation()
                .onInstance(new IdDt("MeasureReport", "measure-asf"))
                .named("$submit-data")
                .withParameters(inParams)
                .execute();

        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();

        Assert.assertTrue(!response.isEmpty());

        Parameters.ParametersParameterComponent component = response.get(0);

        Assert.assertTrue(component.getResource() instanceof Bundle);

        Bundle transactionResponse = (Bundle) component.getResource();

        Assert.assertTrue(transactionResponse.hasType() && transactionResponse.getType() == Bundle.BundleType.TRANSACTIONRESPONSE);
    }
}
