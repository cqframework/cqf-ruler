package org.opencds.cqf.servlet;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.exceptions.FHIRException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.CqlLibraryReader;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.Tuple;
import org.opencds.cqf.helpers.CdsHooksHelper;
import org.opencds.cqf.providers.OmtkDataProvider;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Collectors;

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
        JSONObject requestBody;
        try {
            requestBody = (JSONObject) new JSONParser().parse(request.getReader());
        } catch (ParseException e) {
            response.getWriter().println(e.getMessage());
            return;
        }
        JSONObject prefetch = (JSONObject) requestBody.get("prefetch");
        JSONObject medication = (JSONObject) prefetch.get("medication");
        JSONObject medicationRequestJson = (JSONObject) medication.get("resource");

        // create resource
        FhirContext fhirContext = FhirContext.forDstu3();
        MedicationRequest medicationRequest = (MedicationRequest) fhirContext.newJsonParser().parseResource(medicationRequestJson.toJSONString());

        // get the parameters
        Code rxNormCode;
        try {
            rxNormCode = resolveRxNormCode(medicationRequest);
        } catch (FHIRException e) {
            response.getWriter().println(e.getMessage());
            return;
        }
        Integer rxQuantity = resolveRxQuantity(medicationRequest);
        Integer rxDaysSupply = resolveRxDaysSupply(medicationRequest);

        // get the library
        Library library;
        try {
            library = CqlLibraryReader.read(new File(Paths.get("src/main/resources/OMTKLogic-0.1.0.xml").toAbsolutePath().toString()));
        } catch (JAXBException e) {
            response.getWriter().println(e.getMessage());
            return;
        }

        // set parameters
        Context context = new Context(library);
        context.setParameter(null, "RxNormCode", rxNormCode);
        context.setParameter(null, "RxQuantity", rxQuantity);
        context.setParameter(null, "RxDaysSupply", rxDaysSupply);

        // resolve data provider
        String path = Paths.get("src/main/resources/OpioidManagementTerminologyKnowledge.accdb").toAbsolutePath().toString().replace("\\", "/");
        String connString = "jdbc:ucanaccess://" + path + ";memory=false;keepMirror=true";
        OmtkDataProvider provider = new OmtkDataProvider(connString);
        context.registerDataProvider("http://org.opencds/opioid-cds", provider);

        // get result from the EvaluateMMEs expression
        Object result = context.resolveExpressionRef("TestCalculateMMEs").getExpression().evaluate(context);

        // analyze result and create cards
        JSONArray cards = resolveCards(result);

        // issue response
        JSONObject responseCards = new JSONObject();
        responseCards.put("cards", cards);
        response.getWriter().println(CdsHooksHelper.getPrettyJson(responseCards.toJSONString()));
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CdsHooksHelper.DisplayDiscovery(response);
    }

    private Code resolveRxNormCode(MedicationRequest request) throws FHIRException {
        if (request.hasMedicationCodeableConcept()) {
            Coding concept = request.getMedicationCodeableConcept().getCodingFirstRep();
            return new Code().withCode(concept.getCode()).withSystem(concept.getSystem());
        }

        throw new IllegalArgumentException("Due to performance requirments, MedicationRequest resource must provide the medicationCodeableConcept attribute...");
    }

    private Integer resolveRxQuantity(MedicationRequest request) {

        if (request.hasDispenseRequest()) {
            if (request.getDispenseRequest().hasQuantity()) {
                return request.getDispenseRequest().getQuantity().getValue().intValue();
            }
        }

        throw new IllegalArgumentException("dispenseRequest quantity must be provided in resource!");
    }

    private Integer resolveRxDaysSupply(MedicationRequest request) {

        if (request.hasDispenseRequest()) {
            if (request.getDispenseRequest().hasExpectedSupplyDuration()) {
                return request.getDispenseRequest().getExpectedSupplyDuration().getValue().intValue();
            }
        }

        throw new IllegalArgumentException("dispenseRequest expectedSupplyDuration must be provided in resource!");
    }

    private JSONArray resolveCards(Object result) {
        Iterator it = ((Iterable) result).iterator();
        Tuple tuple = (Tuple) it.next();
        BigDecimal mme = ((org.opencds.cqf.cql.runtime.Quantity)tuple.getElements().get("mme")).getValue();
        String summary, indicator, detail;

        if (mme.compareTo(new BigDecimal("50")) >= 0 && mme.compareTo(new BigDecimal("90")) < 0) {
            summary = "High risk for opioid overdose - consider tapering.";
            detail = String.format("Total morphine milligram equivalent (MME) is %s mg/day. Taper to less than 50.", mme.toString());
            indicator = "warning";
        }

        else if (mme.compareTo(new BigDecimal("90")) > 0) {
            summary = "High risk for opioid overdose - taper now.";
            detail = String.format("Total morphine milligram equivalent (MME) is %s mg/day. Taper to less than 50.", mme.toString());
            indicator = "warning";
        }

        else {
            summary = "Success";
            detail = String.format("The MME %s is less than 50 MME/day", mme.toString());
            indicator = "success";
        }

        JSONArray links = new JSONArray();
        JSONObject govGuideline = new JSONObject();
        JSONObject mmePdf = new JSONObject();

        govGuideline.put("label", "CDC guidance");
        govGuideline.put("url", "https://guidelines.gov/summaries/summary/50153/cdc-guideline-for-prescribing-opioids-for-chronic-pain---united-states-2016#420");
        govGuideline.put("type", "absolute");

        links.add(govGuideline);

        mmePdf.put("label", "MME conversion table");
        mmePdf.put("url", "https://www.cdc.gov/drugoverdose/pdf/calculating_total_daily_dose-a.pdf");
        mmePdf.put("type", "absolute");

        links.add(mmePdf);

        JSONArray cards = new JSONArray();
        JSONObject content = new JSONObject();

        content.put("summary", summary);
        content.put("indicator", indicator);
        content.put("detail", detail);
        content.put("links", links);

        cards.add(content);

        return cards;
    }
}
