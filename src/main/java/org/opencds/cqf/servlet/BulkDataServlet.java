package org.opencds.cqf.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.rp.dstu3.GroupResourceProvider;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.param.StringAndListParam;
import org.hl7.fhir.dstu3.model.Resource;
import org.opencds.cqf.bulkdata.BulkDataRequest;
import org.opencds.cqf.bulkdata.BulkDataResponse;
import org.opencds.cqf.providers.JpaDataProvider;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "bulk-data")
public class BulkDataServlet extends BaseServlet {

    private static Map<String, BulkDataRequest> requests = new HashMap<>();
    public static void registerRequest(String id, String requestUrl, Date since, StringAndListParam type, JpaDataProvider provider) {
        requests.put(id, new BulkDataRequest(id, requestUrl, since, type, provider));
    }
    public static void fireRequest(String id) {
        if (requests.containsKey(id)) {
            new Thread(requests.get(id)).start();
        }
    }

    private static Map<String, BulkDataResponse> responses = new HashMap<>();
    public static void registerResponse(String id, BulkDataResponse response) {
        responses.put(id, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (request.getPathInfo().lastIndexOf("/") > request.getPathInfo().indexOf("/")) {
            String[] idAndType = request.getPathInfo().replaceFirst("/", "").split("/");
            response.getWriter().println(responses.get(idAndType[0]).getResources().get(idAndType[1]));
        }
        else {
            String id = request.getPathInfo().replace("/", "");
            if (!requests.containsKey(id)) {
                response.setStatus(404);
                response.getWriter().println("Unknown request id: " + id);
            } else if (responses.containsKey(id)) {
                response.getWriter().println(responses.get(id).getJson());
            } else {
                response.setIntHeader("Retry-After", 60);
                response.setStatus(102);
            }
        }
        response.getWriter().flush();
    }
}
