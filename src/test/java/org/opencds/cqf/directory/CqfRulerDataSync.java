package org.opencds.cqf.directory;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Test;
import org.opencds.cqf.TestUtil;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;

import static org.junit.Assert.assertTrue;

public class CqfRulerDataSync {
    @Test
    public void syncData() throws Exception {
        String url = "http://measure.eval.kanvix.com/cqf-ruler/baseDstu3";
        TestUtil.startServer();
        IGenericClient client = TestUtil.getCtx().newRestfulGenericClient(url);


        storeAll(url, "Organization");
        storeAll(url, "Practitioner");
        storeAll(url, "Patient");
        storeAll(url, "Encounter");
        storeAll(url, "Observation");
        storeAll(url, "Library");
        storeAll(url, "PlanDefinition");
        storeAll(url, "ActivityDefinition");
        storeAll(url, "Condition");
        storeAll(url, "Procedure");
        storeAll(url, "DiagnosticReport");
//        storeAll(url, "DiagnosticReport");
//        Bundle observationBundle = retrieveAllPatientEverything(url, "Observation");

    }
    private void storeAll( String baseUrl, String resourceId ) throws Exception {
        retrieveAllPatientEverything( baseUrl, resourceId)
            .getEntry().stream()
            .map( bundleEntryComponent -> bundleEntryComponent.getResource())
            .forEach( resource -> {
                try {
                    System.out.println(resourceId+":"+resource.getId());
                    TestUtil.putResource(resource);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

    }
    private static Bundle retrieveAllPatientEverything( String baseUrl, String resourceId ) throws Exception {
        HttpClient httpClient = HttpClientBuilder.create().build();

        String url = baseUrl+"/"+resourceId ;
        HttpGet get        = new HttpGet( url );

        HttpResponse response = httpClient.execute(get);
        InputStream inputStream = response.getEntity().getContent();

        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";

        IBaseResource iBaseResource = TestUtil.getCtx().newJsonParser().parseResource(result);
        assertTrue( iBaseResource instanceof Bundle );
        org.hl7.fhir.dstu3.model.Bundle bundle = (Bundle)iBaseResource;

        AsyncResult asyncResult = new AsyncResult();
        asyncResult.addBundle(bundle);
        AsyncSession.processBundle( asyncResult, bundle, new HashMap<String,String>());

        return asyncResult.resultBundle;
    }

}
