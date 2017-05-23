package org.opencds.cqf.servlet;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opencds.cqf.cql.data.fhir.FhirDataProvider;
import org.opencds.cqf.helpers.CdsCard;
import org.opencds.cqf.helpers.CdsHooksHelper;
import org.opencds.cqf.helpers.CdsRequest;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christopher Schuler on 5/1/2017.
 */
@WebServlet(name = "opioid-guidance-servlet")
public class OpioidGuidanceServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

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
        // TODO: remove HTTP call and call implementation ... somehow
        Parameters contextParams = new Parameters();
        contextParams.addParameter().setName("contextResources").setResource(cdsRequest.getContext());
        contextParams.addParameter().setName("prefetch").setResource(cdsRequest.getPrefetch());
        contextParams.addParameter().setName("source").setValue(new StringType(cdsRequest.getFhirServer()));

        Parameters inParams = new Parameters();
        inParams.addParameter().setName("patient").setValue(new StringType(cdsRequest.getPatient()));
        inParams.addParameter().setName("context").setResource(contextParams);

        FhirDataProvider dstu3Provider = new FhirDataProvider()
                .withEndpoint("http://localhost:8080/cqf-ruler/baseDstu3");

        Parameters careplanParams = dstu3Provider.getFhirClient()
                .operation()
                .onInstance(new IdType("PlanDefinition", "cdc-opioid-05"))
                .named("$apply")
                .withParameters(inParams)
                .execute();

        CarePlan careplan = (CarePlan) careplanParams.getParameterFirstRep().getResource();

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
        throw new ServletException("This servlet is not configured to handle GET requests.");
    }
}
