package org.opencds.cqf.cdshooks.providers;

import ca.uhn.fhir.rest.gclient.ICriterion;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryItem {

    private String itemNo;
    private String resource;
    private String url;
    private List<ICriterion> criteria;
    private boolean patientCriteria;
    private String patientPath;

    public DiscoveryItem() {
        criteria = new ArrayList<>();
        patientCriteria = false;
    }

    public String getItemNo() {
        return itemNo;
    }
    public String getResource() {
        return resource;
    }
    public String getUrl() {
        return url;
    }
    public List<ICriterion> getCriteria() {
        return criteria;
    }
    public boolean isPatientCriteria() {
        return patientCriteria;
    }
    public String getPatientPath() {
        return patientPath;
    }

    public DiscoveryItem setItemNo(int num) {
        this.itemNo = "item" + Integer.toString(num);
        return this;
    }
    public DiscoveryItem setResource(String resource) {
        this.resource = resource;
        return this;
    }
    public DiscoveryItem setUrl(String url) {
        this.url = url;
        return this;
    }
    public DiscoveryItem hasPatientCriteria() {
        this.patientCriteria = true;
        return this;
    }
    public DiscoveryItem setPatientPath(String patientPath) {
        this.patientPath = patientPath;
        return this;
    }

    public void addCriteria(ICriterion criterion) {
        criteria.add(criterion);
    }
}
