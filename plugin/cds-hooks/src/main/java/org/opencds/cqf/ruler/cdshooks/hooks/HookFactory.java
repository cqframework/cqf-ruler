package org.opencds.cqf.ruler.cdshooks.hooks;

import org.opencds.cqf.ruler.cdshooks.request.Request;
import org.opencds.cqf.ruler.cdshooks.exceptions.InvalidHookException;

public class HookFactory {

    public static Hook createHook(Request request) {
        switch (request.getHook()) {
            case "patient-view": return new PatientViewHook(request);
            case "medication-prescribe": return new MedicationPrescribeHook(request);
            case "order-review": return new OrderReviewHook(request);
            case "order-select": return new OrderSelectHook(request);
            case "order-sign": return new OrderSignHook(request);
            default: throw new InvalidHookException("Unknown Hook: " + request.getHook());
        }
    }
}
