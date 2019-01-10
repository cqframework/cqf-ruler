package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.rp.dstu3.PatientResourceProvider;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateParam;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.opencds.cqf.helpers.BulkDataHelper;
import org.opencds.cqf.servlet.BulkDataServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class BulkDataPatientProvider extends PatientResourceProvider {

    private JpaDataProvider provider;
    public BulkDataPatientProvider(JpaDataProvider provider) {
        this.provider = provider;
    }

    @Operation(name = "$export", idempotent = true)
    public OperationOutcome exportAllPatientData(
            javax.servlet.http.HttpServletRequest theServletRequest,
            RequestDetails theRequestDetails,
            HttpServletResponse theServletResponse,
            @OperationParam(name="_outputFormat") String outputFormat,
            @OperationParam(name="_since") DateParam since,
            @OperationParam(name="_type") StringAndListParam type) throws ServletException, IOException
    {
        BulkDataHelper helper = new BulkDataHelper(provider);

        if (theRequestDetails.getHeader("Accept") == null) {
            return helper.createErrorOutcome("Please provide the Accept header, which must be set to application/fhir+json");
        }
        else if (!theRequestDetails.getHeader("Accept").equals("application/fhir+json")) {
            return helper.createErrorOutcome("Only the application/fhir+json value for the Accept header is currently supported");
        }
        if (theRequestDetails.getHeader("Prefer") == null) {
            return helper.createErrorOutcome("Please provide the Prefer header, which must be set to respond-async");
        }
        else if (!theRequestDetails.getHeader("Prefer").equals("respond-async")) {
            return helper.createErrorOutcome("Only the respond-async value for the Prefer header is currently supported");
        }

        if (outputFormat != null) {
            if (!(outputFormat.equals("application/fhir+ndjson")
                    || outputFormat.equals("application/ndjson")
                    || outputFormat.equals("ndjson")))
            {
                return helper.createErrorOutcome("Only ndjson for the _outputFormat parameter is currently supported");
            }
        }

        theServletResponse.setStatus(HttpServletResponse.SC_ACCEPTED);

        String id = UUID.randomUUID().toString();
        BulkDataServlet.registerRequest(id, theRequestDetails.getCompleteUrl(), since == null ? null : since.getValue(), type, provider);
        BulkDataServlet.fireRequest(id);

        theServletResponse.addHeader("Content-Location", System.getProperty("fhir.baseurl.dstu3") + "/export-results/" + id);

        return new OperationOutcome();
    }
}
