package org.opencds.cqf.helpers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.SimpleQuantityDt;
import ca.uhn.fhir.model.dstu2.resource.*;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.parser.DataFormatException;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.exceptions.FHIRException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christopher on 5/15/2017.
 */
public class CdsRequest {

    /*
    * NOTE:
    * This class is configured to ONLY handle the medication-prescribe cds hooks service request
    */

    private String instance;
    private String fhirServer;
    private String hook;
    private String redirect;
    private String user;
    private Bundle context;
    private String patient;
    private Bundle prefetch;

    public CdsRequest() {
        this.context = new Bundle();
        this.prefetch = new Bundle();
    }

    public CdsRequest(JSONObject request) {
        if (!init(request)) {
            throw new IllegalArgumentException("Failed to parse request JSON.");
        }
    }

    private boolean init(JSONObject request) {
        this.instance = request.containsKey("hookInstance") ? request.get("hookInstance").toString() : "";
        this.fhirServer = request.containsKey("fhirServer") ? request.get("fhirServer").toString() : "";
        this.hook = request.containsKey("hook") ? request.get("hook").toString() : "";
        this.redirect = request.containsKey("redirect") ? request.get("redirect").toString() : "";
        this.user = request.containsKey("user") ? request.get("user").toString() : "";

        // context is required in this implementation
        if (!request.containsKey("context")) {
            throw new IllegalArgumentException("The cds request must provide context");
        }

        this.context = new Bundle();
        List<Bundle.BundleEntryComponent> contextResources = new ArrayList<>();
        for (Object obj : (JSONArray) request.get("context")) {
            Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent().setResource(getResource(obj));
            contextResources.add(component);
        }
        this.context.setEntry(contextResources);

        this.patient = request.containsKey("patient") ? request.get("patient").toString() : "";

        this.prefetch = new Bundle();
        if (request.containsKey("prefetch")) {
            List<Bundle.BundleEntryComponent> prefetchResources = new ArrayList<>();
            JSONObject prefetchJSON = (JSONObject) request.get("prefetch");

            Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
            // TODO: generify this
            if (prefetchJSON.containsKey("medication")) {
                JSONObject medication = (JSONObject) prefetchJSON.get("medication");
                component.setResource(getResource(medication.get("resource")));
            }
            prefetchResources.add(component);
            this.prefetch.setEntry(prefetchResources);
        }

        return true;
    }

    private Resource getResource(Object resource) {
        // return ONLY STU3 resources
        Object ret;
        try {
            ret = FhirContext.forDstu2().newJsonParser().parseResource(resource.toString());
            ret = getStu3(ret);
        } catch (DataFormatException | FHIRException e) {
            try {
                ret = FhirContext.forDstu3().newJsonParser().parseResource(resource.toString());
            } catch (DataFormatException dfe) {
                throw new IllegalArgumentException("Invalid resource caused error " + dfe.getMessage());
            }
        }
        return (Resource) ret;
    }

    public MedicationRequest getStu3(Object dstu2Resource) throws FHIRException {
        String resourceName = ((BaseResource) dstu2Resource).getResourceName();

        // TODO: generify this logic
        switch (resourceName) {
            case "MedicationOrder": return convertToMedicationRequest((MedicationOrder) dstu2Resource);
            default:
                throw new IllegalArgumentException("This operation is not configured to handle Resources of type " + resourceName);
        }
    }

    private MedicationRequest convertToMedicationRequest(MedicationOrder order) throws FHIRException {
        /*
        *   Required fields:
        *   MedicationOrder -> MedicationRequest
        *   medication -> medication
        *   dispenseRequest (Backbone) -> Dosage (Element)
        */
        return new MedicationRequest()
                .setStatus(MedicationRequest.MedicationRequestStatus.fromCode(order.getStatus()))
                .setMedication(convertToCodeableConcept((CodeableConceptDt) order.getMedication()))
                .setDosageInstruction(convertToDosage(order.getDosageInstruction()));
    }

    private CodeableConcept convertToCodeableConcept(CodeableConceptDt conceptDt) {
        CodeableConcept concept = new CodeableConcept().setText(conceptDt.getText());
        concept.setId(conceptDt.getElementSpecificId());
        List<Coding> codes = new ArrayList<>();
        for (CodingDt code : conceptDt.getCoding()) {
            codes.add(new Coding()
                    .setCode(code.getCode())
                    .setSystem(code.getSystem())
                    .setDisplay(code.getDisplay())
                    .setVersion(code.getVersion())
            );
        }
        return concept.setCoding(codes);
    }

    public List<Dosage> convertToDosage(List<MedicationOrder.DosageInstruction> instructions) throws FHIRException {
        List<Dosage> dosages = new ArrayList<>();

        for (MedicationOrder.DosageInstruction dosageInstruction : instructions) {
            Dosage dosage = new Dosage();
            dosage.setText(dosageInstruction.getText());
            dosage.setAsNeeded(new BooleanType(((BooleanDt) dosageInstruction.getAsNeeded()).getValue()));


            Integer frequency = dosageInstruction.getTiming().getRepeat().getFrequency();
            Integer frequencyMax = dosageInstruction.getTiming().getRepeat().getFrequencyMax();

            Timing.TimingRepeatComponent repeat = new Timing.TimingRepeatComponent();
            if (frequency != null) {
                repeat.setFrequency(frequency);
            }
            if (frequencyMax != null) {
                repeat.setFrequencyMax(frequencyMax);
            }
            repeat.setPeriod(dosageInstruction.getTiming().getRepeat().getPeriod())
                    .setPeriodUnit(Timing.UnitsOfTime.fromCode(dosageInstruction.getTiming().getRepeat().getPeriodUnits()));

            Timing timing = new Timing();
            timing.setRepeat(repeat);
            dosage.setTiming(timing);

            SimpleQuantityDt quantityDt = (SimpleQuantityDt) dosageInstruction.getDose();
            dosage.setDose(new SimpleQuantity()
                    .setValue(quantityDt.getValue())
                    .setUnit(quantityDt.getUnit())
                    .setCode(quantityDt.getCode())
                    .setSystem(quantityDt.getSystem())
            );

            dosages.add(dosage);
        }

        return dosages;
    }

    public String getInstance() {
        return instance;
    }

    public CdsRequest setInstance(String instance) {
        this.instance = instance;
        return this;
    }

    public String getFhirServer() {
        return fhirServer;
    }

    public CdsRequest setFhirServer(String fhirServer) {
        this.fhirServer = fhirServer;
        return this;
    }

    public String getHook() {
        return hook;
    }

    public CdsRequest setHook(String hook) {
        this.hook = hook;
        return this;
    }

    public String getRedirect() {
        return redirect;
    }

    public CdsRequest setRedirect(String redirect) {
        this.redirect = redirect;
        return this;
    }

    public String getUser() {
        return user;
    }

    public CdsRequest setUser(String user) {
        this.user = user;
        return this;
    }

    public Bundle getContext() {
        return context;
    }

    public CdsRequest setContext(Bundle context) {
        this.context = context;
        return this;
    }

    public String getPatient() {
        return patient;
    }

    public CdsRequest setPatient(String patient) {
        this.patient = patient;
        return this;
    }

    public Bundle getPrefetch() {
        return prefetch;
    }

    public CdsRequest setPrefetch(Bundle prefetch) {
        this.prefetch = prefetch;
        return this;
    }

    public CdsRequest setPrefetch(ca.uhn.fhir.model.dstu2.resource.Bundle prefetch) throws FHIRException {
        // convert to STU3 bundle
        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();

        for (ca.uhn.fhir.model.dstu2.resource.Bundle.Entry entry : prefetch.getEntry()) {
            Bundle.BundleEntryComponent component =
                    new Bundle.BundleEntryComponent().setResource(getStu3(entry.getResource()));
            entries.add(component);
        }
        bundle.setEntry(entries);
        this.prefetch = bundle;
        return this;
    }
}
