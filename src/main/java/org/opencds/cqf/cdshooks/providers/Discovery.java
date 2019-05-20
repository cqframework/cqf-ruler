package org.opencds.cqf.cdshooks.providers;

import org.hl7.fhir.dstu3.model.PlanDefinition;

import java.util.ArrayList;
import java.util.List;

public class Discovery {

    private PlanDefinition planDefinition;
    private List<DiscoveryItem> items;
    private int count;

    public Discovery() {
        items = new ArrayList<>();
        count = 0;
    }

    public PlanDefinition getPlanDefinition() {
        return planDefinition;
    }
    public List<DiscoveryItem> getItems() {
        return items;
    }

    public Discovery setPlanDefinition(PlanDefinition planDefinition) {
        this.planDefinition = planDefinition;
        return this;
    }

    public void addItem(DiscoveryItem item) {
        items.add(item);
    }
    public DiscoveryItem newItem() {
        return new DiscoveryItem().setItemNo(++count);
    }
}
