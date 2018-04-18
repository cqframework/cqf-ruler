package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.opencds.cqf.cdshooks.providers.CdsHooksProviders;
import org.opencds.cqf.exceptions.InvalidContextException;
import org.opencds.cqf.exceptions.MissingRequiredFieldException;

import java.util.List;

public class MedicationPrescribeContext extends Context {

    private List<Object> medicationRequests;
    private JsonElement medications;

    public MedicationPrescribeContext(JsonObject context) {
        super(context);
    }

    @Override
    public List<Object> getResources(CdsHooksProviders.FhirVersion version) {
        if (medicationRequests == null) {
            medicationRequests = CdsHooksHelper.parseResources(medications, version);
        }
        return medicationRequests;
    }

    @Override
    public void validate() {
        medications = getContext().get("medications");
        if (medications == null) {
            throw new MissingRequiredFieldException("The context for the medication-prescribe hook must include a medications field");
        }
        for (String key : getContext().keySet()) {
            if (!key.equals("patientId") && !key.equals("encounterId") && !key.equals("medications")) {
                throw new InvalidContextException("Invalid medication-prescribe context field: " + key + ", expecting patientId, encounterId, or medications");
            }
        }
    }
}
