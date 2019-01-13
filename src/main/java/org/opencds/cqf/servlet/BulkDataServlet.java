package org.opencds.cqf.servlet;

import ca.uhn.fhir.rest.param.StringAndListParam;
import org.hl7.fhir.dstu3.model.Group;
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
import java.util.Map;

@WebServlet(name = "bulk-data")
public class BulkDataServlet extends BaseServlet {

    private static Map<String, BulkDataRequest> requests = new HashMap<>();
    public synchronized static void registerRequest(String id, String requestUrl, Date since, StringAndListParam type, JpaDataProvider provider) {
        requests.put(id, new BulkDataRequest(id, requestUrl, since, type, provider));
    }
    public synchronized static void registerRequest(String id, String requestUrl, Date since, StringAndListParam type, Group group, JpaDataProvider provider) {
        requests.put(id, new BulkDataRequest(id, requestUrl, since, type, group, provider));
    }
    public synchronized static void fireRequest(String id) {
        if (requests.containsKey(id)) {
            new Thread(requests.get(id)).start();
        }
    }

    private static Map<String, BulkDataResponse> responses = new HashMap<>();
    public synchronized static void registerResponse(String id, BulkDataResponse response) {
        responses.put(id, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (request.getPathInfo().lastIndexOf("/") > request.getPathInfo().indexOf("/")) {
            String[] idAndType = request.getPathInfo().replaceFirst("/", "").split("/");
            if (idAndType[1].contains("error")) {
                int idx = Integer.valueOf(idAndType[1].split("_")[1]) - 1;
                response.getWriter().println(responses.get(idAndType[0]).getError().get(idx));
            }
            else {
                response.getWriter().println(responses.get(idAndType[0]).getResources().get(idAndType[1]));
            }
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
