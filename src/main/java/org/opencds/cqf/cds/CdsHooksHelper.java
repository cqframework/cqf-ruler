package org.opencds.cqf.cds;

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

        JSONObject opioidGuidance = new JSONObject();
        opioidGuidance.put("hook", "medication-prescribe");
        opioidGuidance.put("name", "Opioid Morphine Milligram Equivalence (MME) Guidance Service");
        opioidGuidance.put("description", "CDS Service that finds the MME of an opioid medication and provides guidance to the prescriber if the MME exceeds the recommended range.");
        opioidGuidance.put("id", "cdc-opioid-guidance");

        JSONObject prefetchContent = new JSONObject();
        prefetchContent.put("medication", "MedicationOrder?patient={{Patient.id}}&status=active");

        opioidGuidance.put("prefetch", prefetchContent);

        jsonArray.add(opioidGuidance);

//        JSONObject medicationPrescribe = new JSONObject();
//        medicationPrescribe.put("hook", "medication-prescribe");
//        medicationPrescribe.put("name", "User-defined medication-prescribe service");
//        medicationPrescribe.put("description", "Enables user to define a CDS Hooks service using naming conventions");
//        medicationPrescribe.put("id", "user-medication-prescribe");
//
//        medicationPrescribe.put("prefetch", prefetchContent);
//
//        jsonArray.add(medicationPrescribe);
//
        JSONObject zika = new JSONObject();
        zika.put("hook", "patient-view");
        zika.put("name", "Zika Virus Intervention");
        zika.put("description", "Identifies possible Zika exposure and offers suggestions for suggested actions for pregnant patients");
        zika.put("id", "zika-virus-intervention");

        prefetchContent = new JSONObject();
        prefetchContent.put("patient", "Patient/{{Patient.id}}");
        zika.put("prefetch", prefetchContent);

        jsonArray.add(zika);

        JSONObject diabetesManagement = new JSONObject();
        diabetesManagement.put("hook", "patient-view");
        diabetesManagement.put("name", "Diabetes Management");
        diabetesManagement.put("description", "Identifies abnormal lab results and makes suggestions for service requests for diabetic patients");
        diabetesManagement.put("id", "diabetes-management");

        prefetchContent = new JSONObject();
        prefetchContent.put("patient", "Patient/{{Patient.id}}");
        prefetchContent.put("Diabetes Conditions", "Condition?patient={{Patient.id}}&code=250.00,E11.9,313436004,73211009");
        prefetchContent.put("Creatinine Labs", "Observation?patient={{Patient.id}}&code=20005");
        prefetchContent.put("HbA1C Labs", "Observation?patient={{Patient.id}}&code=20006");
        prefetchContent.put("LDL Labs", "Observation?patient={{Patient.id}}&code=20007");
        prefetchContent.put("MicroalbCr Labs", "Observation?patient={{Patient.id}}&code=20008");
        prefetchContent.put("Foot Exams", "Observation?patient={{Patient.id}}&code=20009");
        prefetchContent.put("Eye Exams", "Observation?patient={{Patient.id}}&code=20010");
        prefetchContent.put("ACE or ARB Medications", "MedicationStatement?patient={{Patient.id}}&code=999996");
        diabetesManagement.put("prefetch", prefetchContent);

        jsonArray.add(diabetesManagement);

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
