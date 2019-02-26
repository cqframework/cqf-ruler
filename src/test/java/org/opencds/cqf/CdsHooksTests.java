package org.opencds.cqf;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.MedicationOrder;
import org.junit.Assert;
import org.junit.Test;
import org.opencds.cqf.cdshooks.providers.CdsHooksProviders;
import org.opencds.cqf.cdshooks.request.*;
import org.opencds.cqf.exceptions.InvalidFieldTypeException;
import org.opencds.cqf.exceptions.InvalidHookException;
import org.opencds.cqf.exceptions.InvalidRequestException;
import org.opencds.cqf.exceptions.MissingRequiredFieldException;

import java.io.StringReader;
import java.util.UUID;

public class CdsHooksTests {

    private final String objectOpenBrace = "{";
    private final String objectCloseBrace = "}";
    private final String arrayOpenBrace = "[";
    private final String arrayCloseBrace = "]";
    private final String comma = ",";
    private final String quote = "\"";
    private final String colonSpace = ": ";
    private final String hook = quote + "hook" + quote + colonSpace;
    private final String hookInstance = quote + "hookInstance" + quote + colonSpace;
    private final String fhirServer = quote + "fhirServer" + quote + colonSpace;
    private final String fhirAuth = quote + "fhirAuthorization" + quote + colonSpace + objectOpenBrace;
    private final String accessToken = quote + "access_token" + quote + colonSpace;
    private final String tokenType = quote + "token_type" + quote + colonSpace;
    private final String expiresIn = quote + "expires_in" + quote + colonSpace;
    private final String scope = quote + "scope" + quote + colonSpace;
    private final String subject = quote + "subject" + quote + colonSpace;
    private final String user = quote + "user" + quote + colonSpace;
    private final String context = quote + "context" + quote + colonSpace + objectOpenBrace;
    private final String patientId = quote + "patientId" + quote + colonSpace;
    private final String medicationArray = quote + "medications" + quote + colonSpace + arrayOpenBrace;

    private MedicationRequest request =
            new MedicationRequest()
                    .setIntent(MedicationRequest.MedicationRequestIntent.ORDER)
                    .setMedication(
                            new CodeableConcept().addCoding(
                                    new Coding().setSystem("http://www.nlm.nih.gov/research/umls/rxnorm").setCode("1049502")
                            )
                    )
                    .setSubject(new Reference("Patient/patient-123"));

    @Test
    public void testCdsHooksRequestErrors() {
        CdsRequest request;
        String json;
        String validRequest;
        String service = "example";

        // invalid request JSON - Array
        json = "[]";
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (InvalidRequestException ire) {
            // pass
        }

        // invalid request JSON - Primitive
        json = "123";
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (InvalidRequestException ire) {
            // pass
        }

        // missing hook
        json = objectOpenBrace + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (MissingRequiredFieldException mrfe) {
            // pass
        }

        // invalid hook type
        json = objectOpenBrace + hook + "123" + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (InvalidFieldTypeException ifte) {
            // pass
        }

        // invalid hook
        json = objectOpenBrace + hook + quote + "prescribe-medication" + quote + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (InvalidHookException ihe) {
            // pass
        }

        // missing hook instance
        validRequest = objectOpenBrace + hook + quote + "medication-prescribe" + quote;
        json = validRequest + objectCloseBrace;
        validRequest += comma;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (MissingRequiredFieldException mrfe) {
            // pass
        }

        // invalid hook instance
        validRequest += hookInstance;
        json = validRequest + "null" + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (InvalidFieldTypeException ifte) {
            // pass
        }

        validRequest += quote + UUID.randomUUID() + quote + comma + fhirServer;

        // invalid fhir server
        json = validRequest + quote + "an invalid url.com" + quote + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (InvalidFieldTypeException ifte) {
            // pass
        }

        validRequest += quote + "http://measure.eval.kanvix.com/cqf-ruler/baseDstu3" + quote + comma;

        // invalid fhir auth
        json = validRequest + quote + "fhirAuthorization" + quote + ": []" + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (InvalidFieldTypeException ifte) {
            // pass
        }

        validRequest += fhirAuth;

        // has fhir auth -  missing access token
        json = validRequest + objectCloseBrace + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (MissingRequiredFieldException mrfe) {
            // pass
        }

        // valid fhir auth invalid access token
        validRequest += accessToken;
        json = validRequest + "{}" + objectCloseBrace + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (InvalidFieldTypeException ifte) {
            // pass
        }

        validRequest += quote + "some-opaque-fhir-access-token" + quote;

        // has fhir auth -  missing token type
        json = validRequest + objectCloseBrace + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (MissingRequiredFieldException mrfe) {
            // pass
        }

        validRequest += comma + tokenType;

        // valid fhir auth invalid token type
        json = validRequest + "[]" + objectCloseBrace + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (InvalidFieldTypeException ifte) {
            // pass
        }

        validRequest += quote + "Bearer" + quote;

        // has fhir auth -  missing expires in
        json = validRequest + objectCloseBrace + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (MissingRequiredFieldException mrfe) {
            // pass
        }

        validRequest += comma + expiresIn;

        // valid fhir auth invalid expires in
        json = validRequest + quote + "300" + quote + objectCloseBrace + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (InvalidFieldTypeException ifte) {
            // pass
        }

        validRequest += "300";

        // has fhir auth -  missing scope
        json = validRequest + objectCloseBrace + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (MissingRequiredFieldException mrfe) {
            // pass
        }

        validRequest += comma + scope;

        // valid fhir auth invalid scope
        json = validRequest + "300" + objectCloseBrace + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (InvalidFieldTypeException ifte) {
            // pass
        }

        validRequest += quote + "patient/Patient.read patient/Observation.read" + quote;

        // has fhir auth -  missing subject
        json = validRequest + objectCloseBrace + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (MissingRequiredFieldException mrfe) {
            // pass
        }

        validRequest += comma + subject;

        // valid fhir auth invalid subject
        json = validRequest + "{}" + objectCloseBrace + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (InvalidFieldTypeException ifte) {
            // pass
        }

        validRequest += quote + "cds-service4" + quote + objectCloseBrace;

        // missing user
        json = validRequest + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (MissingRequiredFieldException mrfe) {
            // pass
        }

        validRequest += comma + user;

        // invalid user
        json = validRequest + "123" + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (InvalidFieldTypeException ifte) {
            // pass
        }

        validRequest += quote + "Practitioner/example" + quote;

        // missing context
        json = validRequest + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (MissingRequiredFieldException mrfe) {
            // pass
        }

        validRequest += comma + context;

        // invalid context - missing patientId
        json = validRequest + objectCloseBrace + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (MissingRequiredFieldException mrfe) {
            // pass
        }

        validRequest += patientId + quote + "patient-123" + quote;

        // invalid context - missing medications
        json = validRequest + objectCloseBrace + objectCloseBrace;
        try {
            CdsRequestFactory.createRequest(new StringReader(json));
            Assert.fail();
        } catch (MissingRequiredFieldException mrfe) {
            // pass
        }

        validRequest += comma + medicationArray + FhirContext.forDstu3().newJsonParser().encodeResourceToString(this.request) + arrayCloseBrace + objectCloseBrace;

        // valid request
        json = validRequest + objectCloseBrace;
        CdsRequestFactory.createRequest(new StringReader(json));

        // invalid prefetch
    }

    @Test
    public void testMedicationPrescribeContextStu3() {
        Gson gson = new Gson();
        Bundle bundle = new Bundle().setType(Bundle.BundleType.SEARCHSET);
        bundle.addEntry(new Bundle.BundleEntryComponent().setResource(request));
        String json = String .format("{ \"patientId\": \"patient-123\", \"medications\": %s }", FhirContext.forDstu3().newJsonParser().encodeResourceToString(bundle));

        // Bundle
        Context context = new MedicationPrescribeContext(gson.fromJson(json, JsonObject.class));
        Assert.assertTrue(context.getPatientId().equals("patient-123"));
        Assert.assertTrue(context.getResources(CdsHooksProviders.FhirVersion.DSTU3).size() == 1);
        Assert.assertTrue(((Resource) context.getResources(CdsHooksProviders.FhirVersion.DSTU3).get(0)).fhirType().equals("MedicationRequest"));

        // JSON Array
        json = String .format("{ \"patientId\": \"patient-123\", \"medications\": [%s] }", FhirContext.forDstu3().newJsonParser().encodeResourceToString(request));
        context = new MedicationPrescribeContext(gson.fromJson(json, JsonObject.class));
        Assert.assertTrue(context.getPatientId().equals("patient-123"));
        Assert.assertTrue(context.getResources(CdsHooksProviders.FhirVersion.DSTU3).size() == 1);
        Assert.assertTrue(((Resource) context.getResources(CdsHooksProviders.FhirVersion.DSTU3).get(0)).fhirType().equals("MedicationRequest"));
    }

    @Test
    public void testMedicationPrescribeContextDstu2() {
        Gson gson = new Gson();
        MedicationOrder order =
                new MedicationOrder().setMedication(
                        new org.hl7.fhir.instance.model.CodeableConcept().addCoding(
                                new org.hl7.fhir.instance.model.Coding()
                                        .setSystem("http://www.nlm.nih.gov/research/umls/rxnorm")
                                        .setCode("1049502")
                        )
                );
        org.hl7.fhir.instance.model.Bundle bundle = new org.hl7.fhir.instance.model.Bundle()
                .setType(org.hl7.fhir.instance.model.Bundle.BundleType.SEARCHSET);
        bundle.addEntry(new org.hl7.fhir.instance.model.Bundle.BundleEntryComponent().setResource(order));

        // Bundle
        String json = String .format("{ \"patientId\": \"patient-123\", \"medications\": %s }", FhirContext.forDstu2Hl7Org().newJsonParser().encodeResourceToString(bundle));
        Context context = new MedicationPrescribeContext(gson.fromJson(json, JsonObject.class));
        Assert.assertTrue(context.getPatientId().equals("patient-123"));
        Assert.assertTrue(context.getResources(CdsHooksProviders.FhirVersion.DSTU2).size() == 1);
        Assert.assertTrue(
                ((org.hl7.fhir.instance.model.Resource) context.getResources(CdsHooksProviders.FhirVersion.DSTU2).get(0))
                        .fhirType().equals("MedicationOrder")
        );

        // JSON Array
        json = String .format("{ \"patientId\": \"patient-123\", \"medications\": [%s] }", FhirContext.forDstu2Hl7Org().newJsonParser().encodeResourceToString(order));
        context = new MedicationPrescribeContext(gson.fromJson(json, JsonObject.class));
        Assert.assertTrue(context.getPatientId().equals("patient-123"));
        Assert.assertTrue(context.getResources(CdsHooksProviders.FhirVersion.DSTU2).size() == 1);
        Assert.assertTrue(
                ((org.hl7.fhir.instance.model.Resource) context.getResources(CdsHooksProviders.FhirVersion.DSTU2).get(0))
                        .fhirType().equals("MedicationOrder")
        );
    }
}
