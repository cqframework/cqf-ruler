package org.opencds.cqf.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Christopher Schuler on 5/1/2017.
 */
public class CdsHooksHelper {

    public static void DisplayDiscovery(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        JSONObject jsonResponse = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        JSONObject medicationPrescribe = new JSONObject();
        medicationPrescribe.put("hook", "medication-prescribe");
        medicationPrescribe.put("name", "Opioid Morphine Milligram Equivalence (MME) Guidance Service");
        medicationPrescribe.put("description", "CDS Service that finds the MME of an opioid medication and provides guidance to the prescriber if the MME exceeds the recommended range.");
        medicationPrescribe.put("id", "opioid-mme-guidance");

        JSONObject prefetchContent = new JSONObject();
        // Don't really think I need the patient info...
//        prefetchContent.put("patient", "Patient/{{Patient.id}}");
        prefetchContent.put("medication", "MedicationRequest?patient={{Patient.id}}");

        medicationPrescribe.put("prefetch", prefetchContent);

        jsonArray.add(medicationPrescribe);
        jsonResponse.put("services", jsonArray);

        response.getWriter().println(getPrettyJson(jsonResponse.toJSONString()));
    }

    public static String getPrettyJson(String uglyJson) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(uglyJson);
        return gson.toJson(element);
    }
}
