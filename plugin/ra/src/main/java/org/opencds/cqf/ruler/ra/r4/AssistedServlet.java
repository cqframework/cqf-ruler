package org.opencds.cqf.ruler.ra.r4;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AssistedServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getContentType() == null || !request.getContentType().startsWith("text/csv")) {
            response.setStatus(400);
            response.getWriter().println(String.format(
                    "Invalid content type %s. Please use text/csv.",
                    request.getContentType()));
            return;
        }

        response.setStatus(200);
        response.getWriter().println("Success!");
    }

}
