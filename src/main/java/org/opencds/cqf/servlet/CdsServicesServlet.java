package org.opencds.cqf.servlet;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opencds.cqf.cds.CdsCard;
import org.opencds.cqf.cds.CdsHooksHelper;
import org.opencds.cqf.cds.CdsRequest;
import org.opencds.cqf.providers.PlanDefinitionResourceProvider;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christopher Schuler on 5/1/2017.
 */
@WebServlet(name = "cds-services")
public class CdsServicesServlet extends BaseServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (!request.getRequestURL().toString().endsWith("cdc-opioid-guidance")) {
            throw new ServletException("This endpoint ({Base}/cds-services) is not configured to handle POST requests. Please refer to CDS Hooks documentation (http://cds-hooks.org).");
        }

        // validate that we are dealing with JSON
        if (!request.getContentType().equals("application/json")) {
            throw new ServletException(String.format("Invalid content type %s. Please use application/json.", request.getContentType()));
        }

        // get the request body
        JSONObject requestBody;
        try {
            requestBody = (JSONObject) new JSONParser().parse(request.getReader());
        } catch (ParseException e) {
            response.getWriter().println(e.getMessage());
            return;
        }

        // parse request
        CdsRequest cdsRequest = new CdsRequest(requestBody);

        // If the EHR did not provide the prefetch resources, fetch them
        // Assuming EHR is using DSTU2 resources here...
        // This is a big drag on performance.
        if (cdsRequest.getPrefetch().getEntry().isEmpty()) {
            String searchUrl = String.format("MedicationOrder?patient=%s&status=active", cdsRequest.getPatient());
            ca.uhn.fhir.model.dstu2.resource.Bundle postfetch = FhirContext.forDstu2()
                    .newRestfulGenericClient(cdsRequest.getFhirServer())
                    .search()
                    .byUrl(searchUrl)
                    .returnBundle(ca.uhn.fhir.model.dstu2.resource.Bundle.class)
                    .execute();
            try {
                cdsRequest.setPrefetch(postfetch);
            } catch (FHIRException e) {
                response.getWriter().println(e.getMessage());
            }
        }

        // prepare PlanDefinition $apply operation call
        Parameters contextParams = new Parameters();
        contextParams.addParameter().setName("contextResources").setResource(cdsRequest.getContext());
        contextParams.addParameter().setName("prefetch").setResource(cdsRequest.getPrefetch());
        contextParams.addParameter().setName("source").setValue(new StringType(cdsRequest.getFhirServer()));

        Parameters inParams = new Parameters();
        inParams.addParameter().setName("context").setResource(contextParams);

        PlanDefinitionResourceProvider provider = (PlanDefinitionResourceProvider) getProvider("PlanDefinition");
        CarePlan careplan;
        try {
            careplan = provider.apply(new IdType("cdc-opioid-05"), cdsRequest.getPatient(), null, null, null, null, null, null, null, null, inParams);
        } catch (JAXBException | FHIRException e) {
            throw new IllegalArgumentException("Error occurred during PlanDefinition $apply operation: " + e.getMessage());
        }

        // create the response -- info cards for now
        // TODO: response in form of suggestion -- Resource with more appropriate values
        CdsCard card = new CdsCard();
        for (CarePlan.CarePlanActivityComponent activity : careplan.getActivity()) {
            card.setSummary(careplan.getTitle());
            card.setDetail(careplan.getDescription());
            card.setIndicator(activity.getDetail().getStatusReason());
        }

        List<CdsCard.Links> links = new ArrayList<>();
        for (Annotation annotation : careplan.getNote()) {
            CdsCard.Links link = new CdsCard.Links().setLabel(annotation.getId()).setUrl(annotation.getText()).setType("absolute");
            links.add(link);
        }

        card.setLinks(links);

        String responseCard = CdsHooksHelper.getPrettyJson(card.toJson().toJSONString());
        boolean success = responseCard.isEmpty() || responseCard.equals("{}");

        response.getWriter().println(success ? CdsHooksHelper.getPrettyJson(card.returnSuccess().toJSONString()) : responseCard);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (request.getRequestURL().toString().endsWith("cdc-opioid-guidance")) {
            throw new ServletException("This servlet is not configured to handle GET requests.");
        }

        CdsHooksHelper.DisplayDiscovery(response);
    }
}
