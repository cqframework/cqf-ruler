package org.opencds.cqf.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.rp.dstu3.GroupResourceProvider;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.dstu3.model.Resource;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "bulk-data")
public class BulkDataServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<Resource> resources = (List<Resource>) request.getSession().getAttribute(request.getPathInfo().replace("/", ""));
        IParser parser = FhirContext.forDstu3().newJsonParser();
        StringBuilder builder = new StringBuilder();
        for (Resource resource : resources) {
            builder.append(parser.encodeResourceToString(resource));
        }

        response.getWriter().println(builder.toString().replaceAll("\n", "").replaceAll("\r", ""));
    }
}
