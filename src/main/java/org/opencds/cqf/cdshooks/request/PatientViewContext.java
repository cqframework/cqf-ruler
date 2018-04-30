package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonObject;
import org.opencds.cqf.cdshooks.providers.CdsHooksProviders;
import org.opencds.cqf.exceptions.InvalidContextException;

import java.util.List;

public class PatientViewContext extends Context {

    public PatientViewContext(JsonObject context) {
        super(context);
    }

    @Override
    public List<Object> getResources(CdsHooksProviders.FhirVersion version) {
        return null;
    }

    @Override
    public void validate() {
        for (String key : getContext().keySet()) {
            if (!key.equals("patientId") && !key.equals("encounterId")) {
                throw new InvalidContextException("Invalid patient-view context field: " + key + ", expecting patientId or encounterId");
            }
        }
    }
}
