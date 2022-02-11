package org.opencds.cqf.ruler.cdshooks.discovery;

import com.google.gson.JsonObject;

import org.hl7.fhir.r4.model.PlanDefinition;

public class DiscoveryElementR4 implements DiscoveryElement {
    private PlanDefinition planDefinition;
    private PrefetchUrlList prefetchUrlList;

    public DiscoveryElementR4(PlanDefinition planDefinition, PrefetchUrlList prefetchUrlList) {
        this.planDefinition = planDefinition;
        this.prefetchUrlList = prefetchUrlList;
    }

    public JsonObject getAsJson() {
        JsonObject service = new JsonObject();
        if (planDefinition != null) {
            if (planDefinition.hasAction()) {
                // TODO - this needs some work - too naive
                if (planDefinition.getActionFirstRep().hasTrigger()) {
                    if (planDefinition.getActionFirstRep().getTriggerFirstRep().hasName()) {
                        service.addProperty("hook", planDefinition.getActionFirstRep().getTriggerFirstRep().getName());
                    }
                }
            }
            if (planDefinition.hasName()) {
                service.addProperty("name", planDefinition.getName());
            }
            if (planDefinition.hasTitle()) {
                service.addProperty("title", planDefinition.getTitle());
            }
            if (planDefinition.hasDescription()) {
                service.addProperty("description", planDefinition.getDescription());
            }
            service.addProperty("id", planDefinition.getIdElement().getIdPart());

            if (prefetchUrlList == null) {
                prefetchUrlList = new PrefetchUrlList();
            }

            JsonObject prefetchContent = new JsonObject();
            int itemNo = 0;
            if (!prefetchUrlList.stream().anyMatch(p -> p.equals("Patient/{{context.patientId}}")
                    || p.equals("Patient?_id={{context.patientId}}")
                    || p.equals("Patient?_id=Patient/{{context.patientId}}"))) {
                prefetchContent.addProperty("item1", "Patient?_id={{context.patientId}}");
                ++itemNo;
            }

            for (String item : prefetchUrlList) {
                prefetchContent.addProperty("item" + Integer.toString(++itemNo), item);
            }
            service.add("prefetch", prefetchContent);

            return service;
        }

        return null;
    }
}
