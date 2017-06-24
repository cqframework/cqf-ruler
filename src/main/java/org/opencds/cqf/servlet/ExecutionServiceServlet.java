package org.opencds.cqf.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.elm.execution.ExpressionDef;
import org.cqframework.cql.elm.execution.Library;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.config.STU3LibrarySourceProvider;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.helpers.LibraryHelper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Created by Christopher on 6/22/2017.
 */
@WebServlet(name="evaluate")
public class ExecutionServiceServlet extends BaseServlet {

    private Map<String, List<Integer>> locations = new HashMap<>();

    private LibraryManager libraryManager;
    private LibraryManager getLibraryManager() {
        if (libraryManager == null) {
            libraryManager = new LibraryManager();
            libraryManager.getLibrarySourceLoader().clearProviders();
            libraryManager.getLibrarySourceLoader().registerProvider(getLibrarySourceProvider());
        }
        return libraryManager;
    }

    private LibraryLoader libraryLoader;
    private LibraryLoader getLibraryLoader() {
        if (libraryLoader == null) {
            libraryLoader = new STU3LibraryLoader(getLibraryResourceProvider(), getLibraryManager());
        }
        return libraryLoader;
    }

    private STU3LibrarySourceProvider librarySourceProvider;
    private STU3LibrarySourceProvider getLibrarySourceProvider() {
        if (librarySourceProvider == null) {
            librarySourceProvider = new STU3LibrarySourceProvider(getLibraryResourceProvider());
        }
        return librarySourceProvider;
    }

    private LibraryResourceProvider getLibraryResourceProvider() {
        return (LibraryResourceProvider)getProvider("Library");
    }

    private void registerProviders(Context context, String termSvcUrl, String termUser, String termPass,
                                   String dataPvdrURL, String dataUser, String dataPass, String patientId)
    {
        // TODO: plugin authorization for data provider when available

        String defaultEndpoint = "http://measure.eval.kanvix.com/cqf-ruler/baseDstu3";

        BaseFhirDataProvider provider = new FhirDataProviderStu3()
                .setEndpoint(dataPvdrURL == null ? defaultEndpoint : dataPvdrURL);

        FhirTerminologyProvider terminologyProvider = new FhirTerminologyProvider()
                .withEndpoint(termSvcUrl == null ? defaultEndpoint : termSvcUrl);
        if (!termUser.equals("user ID") && !termPass.isEmpty()) {
            terminologyProvider.withBasicAuth(termUser, termPass);
        }

        provider.setTerminologyProvider(terminologyProvider);
        provider.setExpandValueSets(true);
        context.registerDataProvider("http://hl7.org/fhir", provider);
        context.registerLibraryLoader(getLibraryLoader());
        if (!patientId.equals("null") && !patientId.isEmpty()) {
            context.setContextValue(context.getCurrentContext(), patientId);
        }
    }

    private void performRetrieve(Iterable result, JSONObject results) {
        FhirContext fhirContext = FhirContext.forDstu3(); // for JSON parsing
        Iterator it = result.iterator();
        List<Object> findings = new ArrayList<>();
        while (it.hasNext()) {
            // TODO: currently returning full JSON retrieve response -- ugly and unwieldy
            findings.add(fhirContext
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString((org.hl7.fhir.instance.model.api.IBaseResource)it.next()));
        }
        results.put("result", findings.toString());
    }

    private String resolveType(Object result) {
        String type = result == null ? "Null" : result.getClass().getSimpleName();
        switch (type) {
            case "BigDecimal": return "Decimal";
            case "ArrayList": return "List";
            case "FhirBundleCursor": return "Retrieve";
        }
        return type;
    }

    private void setExpressionLocations(org.hl7.elm.r1.Library library) {
        for (org.hl7.elm.r1.ExpressionDef def : library.getStatements().getDef()) {
            int startLine = def.getTrackbacks().isEmpty() ? 0 : def.getExpression().getTrackbacks().get(0).getStartLine();
            int startChar = def.getTrackbacks().isEmpty() ? 0 : def.getExpression().getTrackbacks().get(0).getStartChar();
            List<Integer> loc = Arrays.asList(startLine, startChar);
            locations.put(def.getName(), loc);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // validate that we are dealing with JSON or plain text
        if (!request.getContentType().equals("application/json") && !request.getContentType().equals("text/plain"))
        {
            throw new ServletException(String.format("Invalid content type %s. Please use application/json or text/plain.", request.getContentType()));
        }

        JSONParser parser = new JSONParser();
        JSONObject json;
        try {
            json = (JSONObject) parser.parse(request.getReader());
        } catch (ParseException e) {
            throw new ServletException("Error parsing JSON request: " + e.getMessage());
        }

        String code = (String) json.get("code");
        String fhirServiceUri = (String) json.get("fhirServiceUri");
        String fhirUser = (String) json.get("fhirUser");
        String fhirPass = (String) json.get("fhirPass");
        String dataServiceUri = (String) json.get("dataServiceUri");
        String dataUser = (String) json.get("dataUser");
        String dataPass = (String) json.get("dataPass");
        String patientId = (String) json.get("patientId");

        CqlTranslator translator = LibraryHelper.getTranslator(code, getLibraryManager());
        setExpressionLocations(translator.getTranslatedLibrary().getLibrary());

        Library library;
        try {
            library = LibraryHelper.translateLibrary(translator);
        }
        catch (IllegalArgumentException iae) {
            JSONObject result = new JSONObject();
            JSONArray resultArr = new JSONArray();
            result.put("translation-error", iae.getMessage());
            resultArr.add(result);
            response.getWriter().println(resultArr.toJSONString());
            return;
        }

        Context context = new Context(library);
        registerProviders(context, fhirServiceUri, fhirUser, fhirPass, dataServiceUri, dataUser, dataPass, patientId);

        JSONArray resultArr = new JSONArray();
        for (ExpressionDef def : library.getStatements().getDef()) {
            context.enterContext(def.getContext());
            JSONObject result = new JSONObject();

            try {
                result.put("name", def.getName());

                String location = String.format("[%d:%d]", locations.get(def.getName()).get(0), locations.get(def.getName()).get(1));
                result.put("location", location);

                Object res = def.getExpression().evaluate(context);

                if (res instanceof Iterable) {
                    performRetrieve((Iterable) res, result);
                }
                else {
                    result.put("result", res == null ? "Null" : res.toString());
                }
                result.put("resultType", resolveType(res));
            }
            catch (RuntimeException re) {
                result.put("error", re.getMessage());
            }
            resultArr.add(result);
        }
        response.getWriter().println(resultArr.toJSONString());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        throw new ServletException("This servlet is not configured to handle GET requests.");
    }
}
