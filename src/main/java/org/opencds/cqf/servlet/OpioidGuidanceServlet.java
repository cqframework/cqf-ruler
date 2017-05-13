package org.opencds.cqf.servlet;

import org.hl7.fhir.dstu3.model.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opencds.cqf.cql.data.fhir.FhirDataProvider;
import org.opencds.cqf.helpers.CdsCard;
import org.opencds.cqf.helpers.CdsHooksHelper;

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

        // parse the body
        // prefetch is optional -- for now, to ensure functionality, assume it won't be included in request...
        JSONObject requestBody;
        try {
            requestBody = (JSONObject) new JSONParser().parse(request.getReader());
        } catch (ParseException e) {
            response.getWriter().println(e.getMessage());
            return;
        }

//        JSONObject prefetch = (JSONObject) requestBody.get("prefetch");
//        JSONObject medication = (JSONObject) prefetch.get("medication");
//        JSONObject medicationRequestJson = (JSONObject) medication.get("resource");
//
//        // create resource
//        FhirContext fhirContext = FhirContext.forDstu3();
//        MedicationRequest medicationRequest = (MedicationRequest) fhirContext.newJsonParser().parseResource(medicationRequestJson.toJSONString());

        // get the fhir server info.
        String fhirEndpoint = requestBody.get("fhirServer").toString();
        FhirDataProvider dstu3Provider = new FhirDataProvider().withEndpoint("http://measure.eval.kanvix.com/cqf-ruler/baseDstu3");

        String patient = requestBody.get("patient").toString();
//        String searchUrl = String.format("http://measure.eval.kanvix.com/cqf-ruler/baseDstu3/PlanDefinition/cdc-opioid-05/$evaluate?patient=%s&source=%s", patient, fhirEndpoint);
        Parameters inParams = new Parameters();
        inParams.addParameter().setName("patient").setValue(new StringType(patient));
        inParams.addParameter().setName("source").setValue(new StringType(fhirEndpoint));
        Parameters careplanParams = dstu3Provider.getFhirClient()
                .operation()
                .onInstance(new IdType("PlanDefinition", "cdc-opioid-05"))
                .named("$apply")
                .withParameters(inParams)
                .useHttpGet()
                .execute();
        CarePlan careplan = (CarePlan) careplanParams.getParameterFirstRep().getResource();

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


        response.getWriter().println(CdsHooksHelper.getPrettyJson(card.toJson().toJSONString()));
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CdsHooksHelper.DisplayDiscovery(response);
    }

    private CarePlan getCarePlan(Bundle bundle) {
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            return (CarePlan) entry.getResource();
        }
        return null;
    }
}
