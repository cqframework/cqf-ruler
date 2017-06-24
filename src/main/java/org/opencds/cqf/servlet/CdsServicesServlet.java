package org.opencds.cqf.servlet;

import org.opencds.cqf.cds.CdsHooksHelper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Christopher Schuler on 5/1/2017.
 */
@WebServlet(name = "cds-services")
public class CdsServicesServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        response.getWriter().println("This endpoint ({Base}/cds-services) is not configured to handle POST requests. Please refer to CDS Hooks documentation (http://cds-hooks.org).");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CdsHooksHelper.DisplayDiscovery(response);
    }
}
