package org.opencds.cqf.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
        response.setContentType("application/json");

        JSONObject jsonResponse = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        JSONObject medicationPrescribe = new JSONObject();
        medicationPrescribe.put("hook", "medication-prescribe");
        medicationPrescribe.put("name", "Opioid Morphine Milligram Equivalence (MME) Guidance Service");
        medicationPrescribe.put("description", "CDS Service that finds the MME of an opioid medication and provides guidance to the prescriber if the MME exceeds the recommended range.");
        medicationPrescribe.put("id", "opioid-mme-guidance");

        JSONObject prefetchContent = new JSONObject();
        prefetchContent.put("patient", "Patient/{{Patient.id}}");
        prefetchContent.put("medication", "MedicationRequest?patient={{Patient.id}}");

        medicationPrescribe.put("prefetch", prefetchContent);

        jsonArray.add(medicationPrescribe);
        jsonResponse.put("services", jsonArray);

        // using gson for pretty print ...
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(jsonResponse.toJSONString());

        response.getWriter().println(gson.toJson(element));
    }
}
