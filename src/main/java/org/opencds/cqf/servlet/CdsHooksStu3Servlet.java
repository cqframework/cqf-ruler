package org.opencds.cqf.servlet;

import ca.uhn.fhir.context.FhirVersionEnum;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "cds-services-stu3")
public class CdsHooksStu3Servlet extends CdsHooksServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setVersion(FhirVersionEnum.DSTU3);
        super.doPost(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setVersion(FhirVersionEnum.DSTU3);
        super.doGet(request, response);
    }
}
